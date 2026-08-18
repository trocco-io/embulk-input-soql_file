package org.embulk.input.soql;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import com.fasterxml.jackson.databind.JsonNode;
import com.sforce.async.AsyncApiException;
import com.sforce.async.BatchInfo;
import com.sforce.async.BulkConnection;
import com.sforce.async.JobInfo;
import com.sforce.ws.ConnectionException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigException;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;
import org.embulk.spi.Exec;
import org.embulk.spi.FileInputPlugin;
import org.embulk.spi.TempFileSpace;
import org.embulk.spi.TransactionalFileInput;
import org.embulk.util.config.ConfigMapper;
import org.embulk.util.config.ConfigMapperFactory;
import org.embulk.util.config.TaskMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SoqlFilePlugin implements FileInputPlugin {
    private final Logger logger = LoggerFactory.getLogger(SoqlFilePlugin.class);

    private static final ConfigMapperFactory CONFIG_MAPPER_FACTORY =
            ConfigMapperFactory.builder().addDefaultModules().build();
    private static final ConfigMapper CONFIG_MAPPER = CONFIG_MAPPER_FACTORY.createConfigMapper();
    private static final TaskMapper TASK_MAPPER = CONFIG_MAPPER_FACTORY.createTaskMapper();

    @Override
    public ConfigDiff transaction(ConfigSource config, FileInputPlugin.Control control) {
        final PluginTask task = CONFIG_MAPPER.map(config, PluginTask.class);
        validateConfig(task);

        return buildNextConfigDiff(task, control.run(task.toTaskSource(), 1));
    }

    @Override
    public ConfigDiff resume(
            TaskSource taskSource, int taskCount, FileInputPlugin.Control control) {
        final PluginTask task = TASK_MAPPER.map(taskSource, PluginTask.class);
        validateConfig(task);

        return buildNextConfigDiff(task, control.run(taskSource, taskCount));
    }

    private void validateConfig(PluginTask task) {
        if (task.getAuthMethod() == AuthMethod.user_password
                && !isSoapLoginAvailable(task.getApiVersion())) {
            throw new ConfigException(
                    "auth_method: user_password cannot be used with api_version "
                            + task.getApiVersion()
                            + " because the Salesforce SOAP login() call is not available in API"
                            + " version 65.0 or later. Set api_version to 64.0 or lower, or use"
                            + " auth_method: oauth.");
        }
        if (task.getSoql().isPresent() && task.getSelect().isPresent()) {
            throw new ConfigException("both soql and select are set");
        }
        if (!task.getIncremental() && !task.getSoql().isPresent()) {
            throw new ConfigException("soql must be set if incremental is false");
        }
        if (task.getIncremental()) {
            if (task.getSoql().isPresent()) {
                throw new ConfigException("soql with incremental doesn't support");
            }
            if (task.getIncrementalColumns().isEmpty()) {
                throw new ConfigException("incremental_columns must be set if incremental is true");
            }
        }
    }

    // The SOAP login() call is not available in API version 65.0 or later.
    // https://help.salesforce.com/s/articleView?id=release-notes.rn_api_upcoming_retirement_258rn.htm&language=en_US&release=258&type=5
    private static boolean isSoapLoginAvailable(String apiVersion) {
        try {
            return Double.parseDouble(apiVersion) < 65.0;
        } catch (NumberFormatException e) {
            // Unknown version format; defer to Salesforce to report an error.
            return true;
        }
    }

    private ConfigDiff buildNextConfigDiff(PluginTask task, List<TaskReport> reports) {
        final ConfigDiff next = CONFIG_MAPPER_FACTORY.newConfigDiff();

        if (reports.size() > 0 && reports.get(0).has("last_record")) {
            final TaskReport report = CONFIG_MAPPER_FACTORY.rebuildTaskReport(reports.get(0));
            next.set("last_record", report.get(JsonNode.class, "last_record"));
        } else if (task.getLastRecord().isPresent()) {
            next.set("last_record", task.getLastRecord().get());
        }

        return next;
    }

    @Override
    public void cleanup(
            TaskSource taskSource, int taskCount, List<TaskReport> successTaskReports) {}

    @Override
    public TransactionalFileInput open(TaskSource taskSource, int taskIndex) {
        final PluginTask pluginTask = TASK_MAPPER.map(taskSource, PluginTask.class);

        List<Path> csvFilePaths;
        JobInfo jobInfo = null;
        BulkConnection bulkConnection = null;
        try {
            ForceClient forceClient = createForceClient(pluginTask);
            String soql = buildSoql(pluginTask, forceClient);
            logger.debug("SOQL: {}", soql);
            List<String> recordKeyList = forceClient.query(pluginTask, soql);
            bulkConnection = forceClient.getBulkConnection();
            jobInfo = forceClient.getJobInfo();
            BatchInfo batchInfo = forceClient.getBatchInfo();

            TempFileSpace tempFileSpace = Exec.getTempFileSpace();
            csvFilePaths = new ArrayList<>(recordKeyList.size());
            for (Iterator<String> it = recordKeyList.iterator(); it.hasNext(); ) {
                Path csv = tempFileSpace.createTempFile().toPath();
                csvFilePaths.add(csv);
                NullByteFilterInputStream filtered;
                try (InputStream input =
                        bulkConnection.getQueryResultStream(
                                jobInfo.getId(), batchInfo.getId(), it.next())) {
                    filtered = new NullByteFilterInputStream(input);
                    Files.copy(filtered, csv, REPLACE_EXISTING);
                }
                if (filtered.hasDetectedNullBytes()) {
                    logger.warn(
                            "Null bytes (0x00) were detected and removed from the Bulk API response CSV. "
                                    + "The source Salesforce data may contain invalid characters.");
                }
            }
        } catch (AsyncApiException e) {
            logger.error(e.getMessage(), e);
            throw new ConfigException(e);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        } finally {
            if (bulkConnection != null && jobInfo != null) {
                try {
                    bulkConnection.closeJob(jobInfo.getId());
                } catch (Exception e) {
                    logger.warn("Failed to close Bulk API job", e);
                }
            }
        }

        return new CsvFileInput(pluginTask, csvFilePaths);
    }

    protected ForceClient createForceClient(PluginTask task)
            throws AsyncApiException, ConnectionException {
        return new ForceClient(task);
    }

    private String buildSoql(PluginTask pluginTask, ForceClient forceClient)
            throws ConnectionException {
        validateConfig(pluginTask);

        // soql を利用する場合
        if (!pluginTask.getIncremental() && pluginTask.getSoql().isPresent()) {
            return pluginTask.getSoql().get();
        }

        // select を利用する場合
        String select = pluginTask.getSelect().orElse(null);
        if (select == null) {
            List<String> fields = forceClient.describeObjectFieldNames(pluginTask.getObject());
            select = String.join(",", fields);
        }
        SoqlBuilder soqlBuilder =
                new SoqlBuilder(
                        select,
                        pluginTask.getObject(),
                        pluginTask.getWhere(),
                        pluginTask.getLimit(),
                        pluginTask.getIncrementalColumns(),
                        pluginTask.getLastRecord());
        return soqlBuilder.build();
    }
}
