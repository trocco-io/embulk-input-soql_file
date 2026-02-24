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

        return buildNextConfigDiff(task, control.run(task.toTaskSource(), 1));
    }

    @Override
    public ConfigDiff resume(
            TaskSource taskSource, int taskCount, FileInputPlugin.Control control) {
        final PluginTask task = TASK_MAPPER.map(taskSource, PluginTask.class);

        return buildNextConfigDiff(task, control.run(taskSource, taskCount));
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
            logger.debug("SOQL: " + soql);
            List<String> recordKeyList = forceClient.query(pluginTask, soql);
            bulkConnection = forceClient.getBulkConnection();
            jobInfo = forceClient.getJobInfo();
            BatchInfo batchInfo = forceClient.getBatchInfo();

            TempFileSpace tempFileSpace = Exec.getTempFileSpace();
            csvFilePaths = new ArrayList<>(recordKeyList.size());
            for (Iterator<String> it = recordKeyList.iterator(); it.hasNext(); ) {
                Path csv = tempFileSpace.createTempFile().toPath();
                csvFilePaths.add(csv);
                try (InputStream input =
                        bulkConnection.getQueryResultStream(
                                jobInfo.getId(), batchInfo.getId(), it.next())) {
                    Files.copy(input, csv, REPLACE_EXISTING);
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
        // 不正な incremental, soql, select の組み合わせを事前に検証する
        if (pluginTask.getSoql().isPresent() && pluginTask.getSelect().isPresent()) {
            throw new ConfigException("both soql and select are set");
        }
        if (!pluginTask.getIncremental() && !pluginTask.getSoql().isPresent()) {
            throw new ConfigException("soql must be set if incremental is false");
        }
        if (pluginTask.getIncremental()) {
            if (pluginTask.getSoql().isPresent()) {
                throw new ConfigException("soql with incremental doesn't support");
            }
            if (pluginTask.getIncrementalColumns().isEmpty()) {
                throw new ConfigException("incremental_columns must be set if incremental is true");
            }
        }

        // soql を利用する場合
        if (!pluginTask.getIncremental() && pluginTask.getSoql().isPresent()) {
            return pluginTask.getSoql().get();
        }

        // select を利用する場合
        String select = pluginTask.getSelect().orElse(null);
        if (select == null) {
            List<String> fileds = forceClient.describeObjectFieldNames(pluginTask.getObject());
            select = String.join(",", fileds);
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
