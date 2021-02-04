package org.embulk.input.soql;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import com.sforce.async.AsyncApiException;
import com.sforce.async.BulkConnection;

import org.embulk.exec.ExecutionInterruptedException;
import org.embulk.spi.util.InputStreamFileInput;
import org.embulk.spi.util.InputStreamFileInput.InputStreamWithHints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SingleFileProvider implements InputStreamFileInput.Provider
{
    private final Logger logger = LoggerFactory.getLogger(SingleFileProvider.class);

    private Iterator<String> iterator;
    private BulkConnection bulkConnection;
    private String jobId;
    private String batchId;

    public SingleFileProvider(List<String> recordKeyList, BulkConnection bulkConnection, String jobId, String batchId)
    {
        this.iterator = recordKeyList.iterator();
        this.bulkConnection = bulkConnection;
        this.jobId = jobId;
        this.batchId = batchId;
    }

    @Override
    public InputStream openNext()
    {
        if (!iterator.hasNext()) {
            return null;
        }
        return findPartRecords(iterator.next(), jobId, batchId);
    }

    private InputStream findPartRecords(String resultId, String jobId, String batchId)
    {
        try {
            return bulkConnection.getQueryResultStream(jobId, batchId, resultId);
        }
        catch (AsyncApiException e) {
            logger.error(e.getMessage(), e);
            throw new ExecutionInterruptedException(e);
        }
    }

    @Override
    public void close() throws IOException
    {
    }
}
