package org.embulk.input.soql;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.sforce.async.AsyncApiException;
import com.sforce.async.BatchInfo;
import com.sforce.async.BatchStateEnum;
import com.sforce.async.BulkConnection;
import com.sforce.async.ConcurrencyMode;
import com.sforce.async.ContentType;
import com.sforce.async.JobInfo;
import com.sforce.async.OperationEnum;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;

import org.embulk.config.ConfigException;
import org.embulk.exec.ExecutionInterruptedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ForceClient
 */
public class ForceClient
{

    private final Logger logger =  LoggerFactory.getLogger(ForceClient.class);

    private static final long INITIAL_DELAY = 1;
    private static final long PERIOD = 5;
    private static final int BATCH_STATUS_CHECK_INTERVAL = 10000;

    private BulkConnection bulkConnection;
    private JobInfo jobInfo;
    private BatchInfo batchInfo;

    private final Map<AuthMethod, ConnectorConfigCreater> connectorConfigCreaters = new HashMap<>();

    public ForceClient(PluginTask pluginTask) throws AsyncApiException, ConnectionException
    {
        setConnectorConfigCreaters(pluginTask);
        ConnectorConfigCreater connectorConfigCreater = connectorConfigCreaters.get(pluginTask.getAuthMethod());
        ConnectorConfig connectorConfig = connectorConfigCreater.createConnectorConfig();
        bulkConnection = new BulkConnection(connectorConfig);
    }

    public BulkConnection getBulkConnection()
    {
        return this.bulkConnection;
    }

    public JobInfo getJobInfo()
    {
        return this.jobInfo;
    }

    public BatchInfo getBatchInfo()
    {
        return this.batchInfo;
    }

    public List<String> query(PluginTask pluginTask) throws AsyncApiException, InterruptedException, ExecutionException
    {
        this.jobInfo = createJobInfo(pluginTask.getObject(), pluginTask.getIncludeDeletedOrArchivedRecords());
        this.batchInfo = createBatchInfo(pluginTask.getSoql(), jobInfo);

        CompletableFuture<String[]> result = execBatch(jobInfo, batchInfo);
        return Arrays.asList(result.get());
    }

    private CompletableFuture<String[]> execBatch(
        JobInfo jobInfo,
        BatchInfo batchInfo) throws AsyncApiException
    {
        BatchExecutor batchExecutor = new BatchExecutor(bulkConnection, batchInfo);
        ScheduledExecutorService checkBatchStatus = Executors.newSingleThreadScheduledExecutor();
        checkBatchStatus.scheduleAtFixedRate(batchExecutor, INITIAL_DELAY, PERIOD, TimeUnit.SECONDS);

        CompletableFuture<String[]> result = batchExecutor.getResult();
        result.whenComplete((results, thrown) -> {
            checkBatchStatus.shutdownNow();
        });

        while (!checkBatchStatus.isShutdown()) {
            try {
                Thread.sleep(BATCH_STATUS_CHECK_INTERVAL);
            }
            catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
                throw new ExecutionInterruptedException(e);
            }
        }
        if (notCompleted(batchExecutor)) {
            batchInfo = bulkConnection.getBatchInfo(batchInfo.getJobId(), batchInfo.getId(), ContentType.CSV);
            String msg = String.format("soql batch not completed. batch_id=%s. job_id=%s. batch_state=%s. batch_state_message=%s.", batchInfo.getId(), jobInfo.getId(), batchInfo.getState().toString(), batchInfo.getStateMessage());
            logger.error(msg);
            throw new ConfigException(msg);
        }
        return result;
    }

    private BatchInfo createBatchInfo(
        String soql,
        JobInfo jobInfo) throws AsyncApiException
    {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(soql.getBytes());
        BatchInfo batchInfo = bulkConnection.createBatchFromStream(jobInfo, byteArrayInputStream);
        logger.info("batch_id is {}, job_id is {}", batchInfo.getId(), batchInfo.getJobId());
        return batchInfo;
    }

    private boolean notCompleted(BatchExecutor batchExecutor) throws AsyncApiException
    {
        return batchExecutor.getBatchInfo().getState() != BatchStateEnum.Completed;
    }

    private JobInfo createJobInfo(String object, boolean includeDeletedOrArchivedRecords) throws AsyncApiException
    {
        JobInfo jobInfo = new JobInfo();
        jobInfo.setObject(object);
        OperationEnum operation = includeDeletedOrArchivedRecords ? OperationEnum.queryAll : OperationEnum.query;
        jobInfo.setOperation(operation);
        jobInfo.setConcurrencyMode(ConcurrencyMode.Parallel);
        jobInfo.setContentType(ContentType.CSV);
        jobInfo = bulkConnection.createJob(jobInfo);
        return jobInfo;
    }

    private void setConnectorConfigCreaters(PluginTask pluginTask) {
        connectorConfigCreaters.put(AuthMethod.oauth, new OauthConnectorConfigCreater(pluginTask));
        connectorConfigCreaters.put(AuthMethod.user_password, new UserPasswordConnectorConfigCreater(pluginTask));
    }
}
