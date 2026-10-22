package org.embulk.input.soql;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import com.fasterxml.jackson.databind.JsonNode;
import com.sforce.async.AsyncApiException;
import com.sforce.async.AsyncExceptionCode;
import com.sforce.async.BatchInfo;
import com.sforce.async.BulkConnection;
import com.sforce.async.JobInfo;
import com.sforce.ws.ConnectionException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.net.ssl.SSLHandshakeException;
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

    // Errors returned by Salesforce that are not caused by the configuration and may succeed on
    // another attempt.
    private static final Set<AsyncExceptionCode> TRANSIENT_SERVER_ERROR_CODES =
            Collections.unmodifiableSet(
                    EnumSet.of(
                            AsyncExceptionCode.InternalServerError,
                            AsyncExceptionCode.Timeout,
                            AsyncExceptionCode.TooManyLockFailure));

    // force-wsc throws ClientInputError with this message and without a cause when an error
    // response has an unknown Content-Type, e.g. when the connection is reset before a response.
    // This is a heuristic: an HTML or plain-text error page (e.g. from a proxy) also has this
    // message and may fail again, but force-wsc gives no other signal to tell them apart.
    private static final String UNKNOWN_FORMAT_ERROR_MESSAGE =
            "Server error returned in unknown format";

    // I/O failures that come from the configuration or the environment, such as a wrong
    // instance_url or a certificate problem, and fail again on retry. A DNS lookup can also fail
    // briefly, but a wrong host is the more likely cause, and these were reported as configuration
    // errors before. ConnectException stays transient because a refused or timed-out connection
    // is often temporary.
    private static final List<Class<? extends IOException>> NON_TRANSIENT_IO_FAILURES =
            Collections.unmodifiableList(
                    Arrays.asList(
                            MalformedURLException.class,
                            UnknownHostException.class,
                            SSLHandshakeException.class));

    private static final int DOWNLOAD_RETRY_LIMIT = 3;
    private static final long DOWNLOAD_RETRY_INITIAL_WAIT_MILLIS = 5_000L;

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
        // Validate before creating ForceClient, whose constructor connects to Salesforce.
        // This covers executors that call open() without transaction() in the same process.
        validateConfig(pluginTask);

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
            for (String resultId : recordKeyList) {
                Path csv = tempFileSpace.createTempFile().toPath();
                csvFilePaths.add(csv);
                downloadQueryResult(
                        bulkConnection, jobInfo.getId(), batchInfo.getId(), resultId, csv);
            }
        } catch (AsyncApiException e) {
            logger.error(e.getMessage(), e);
            if (isTransientError(e)) {
                throw new RuntimeException(e);
            }
            throw new ConfigException(e);
        } catch (InterruptedException e) {
            // Keep the interrupt visible to the executor, e.g. while waiting between retries.
            Thread.currentThread().interrupt();
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
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

    // The batch has already completed and its results are kept on Salesforce, so downloading the
    // same result again is safe. Retry only the download instead of failing the whole task.
    private void downloadQueryResult(
            BulkConnection bulkConnection, String jobId, String batchId, String resultId, Path csv)
            throws AsyncApiException, IOException, InterruptedException {
        for (int retryCount = 0; ; retryCount++) {
            try {
                NullByteFilterInputStream filtered;
                try (InputStream input =
                        bulkConnection.getQueryResultStream(jobId, batchId, resultId)) {
                    filtered = new NullByteFilterInputStream(input);
                    Files.copy(filtered, csv, REPLACE_EXISTING);
                }
                if (filtered.hasDetectedNullBytes()) {
                    logger.warn(
                            "Null bytes (0x00) were detected and removed from the Bulk API response CSV. "
                                    + "The source Salesforce data may contain invalid characters.");
                }
                return;
            } catch (AsyncApiException | IOException e) {
                if (retryCount >= DOWNLOAD_RETRY_LIMIT || !isRetryableDownloadError(e)) {
                    throw e;
                }
                long waitMillis = DOWNLOAD_RETRY_INITIAL_WAIT_MILLIS << retryCount;
                logger.warn(
                        "Failed to download query result. result_id={}. Retrying {}/{} after {} ms.",
                        resultId,
                        retryCount + 1,
                        DOWNLOAD_RETRY_LIMIT,
                        waitMillis,
                        e);
                sleep(waitMillis);
            }
        }
    }

    private static boolean isRetryableDownloadError(Exception e) {
        if (e instanceof AsyncApiException) {
            return isTransientError((AsyncApiException) e);
        }
        // IOException while reading the response body, e.g. a connection reset.
        return true;
    }

    private static boolean isTransientError(AsyncApiException e) {
        if (e.getExceptionCode() == AsyncExceptionCode.ClientInputError) {
            return isClientSideIoFailure(e);
        }
        return TRANSIENT_SERVER_ERROR_CODES.contains(e.getExceptionCode());
    }

    // Salesforce also returns ClientInputError for a wrong request, which fails again on retry.
    // Treat it as transient only when force-wsc raised it locally for an I/O failure.
    private static boolean isClientSideIoFailure(AsyncApiException e) {
        Throwable cause = e.getCause();
        if (cause instanceof IOException) {
            return NON_TRANSIENT_IO_FAILURES.stream().noneMatch(type -> type.isInstance(cause));
        }
        return cause == null && UNKNOWN_FORMAT_ERROR_MESSAGE.equals(e.getExceptionMessage());
    }

    protected void sleep(long millis) throws InterruptedException {
        Thread.sleep(millis);
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
