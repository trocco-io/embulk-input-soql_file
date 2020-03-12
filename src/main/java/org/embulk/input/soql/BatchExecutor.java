package org.embulk.input.soql;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.CompletableFuture;

import com.sforce.async.AsyncApiException;
import com.sforce.async.BatchInfo;
import com.sforce.async.BulkConnection;
import com.sforce.async.ContentType;
import com.sforce.async.QueryResultList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BatchExecutor
 */
public class BatchExecutor implements Runnable
{
    private final Logger logger =  LoggerFactory.getLogger(BatchExecutor.class);

    private CompletableFuture<String[]> result = new CompletableFuture<>();
    private BulkConnection bulkConnection;
    private BatchInfo batchInfo;

    public BatchExecutor(BulkConnection bulkConnection, BatchInfo batchInfo)
    {
        this.bulkConnection = bulkConnection;
        this.batchInfo = batchInfo;
    }

    public CompletableFuture<String[]> getResult()
    {
        return result;
    }

    public BatchInfo getBatchInfo() throws AsyncApiException
    {
        return batchInfo;
    }

    @Override
    public void run()
    {
        try {
            this.batchInfo = bulkConnection.getBatchInfo(batchInfo.getJobId(), batchInfo.getId(), ContentType.CSV);
            switch (batchInfo.getState()) {
                case Completed:
                    logger.info("batch completed");
                    QueryResultList queryResultList =  bulkConnection.getQueryResultList(batchInfo.getJobId(), batchInfo.getId(), ContentType.CSV);
                    result.complete(queryResultList.getResult());
                    break;
                case Failed:
                    logger.info("soql batch error. batch_state_message={}.", batchInfo.getState().toString());
                    result.complete(null);
                    break;
                default:
                    logger.info("soql batch running. batch_state_message={}.", batchInfo.getState().toString());
                    break;
            }
        }
        catch (AsyncApiException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            logger.error(sw.toString());
            result.complete(null);
        }
    }
}
