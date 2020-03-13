package org.embulk.input.soql;

import java.util.List;

import com.sforce.async.BulkConnection;

import org.embulk.config.TaskReport;
import org.embulk.spi.Exec;
import org.embulk.spi.TransactionalFileInput;
import org.embulk.spi.util.InputStreamFileInput;

public class SoqlFileInput extends InputStreamFileInput implements TransactionalFileInput
{

  public SoqlFileInput(PluginTask task, List<String> recordKeyList, BulkConnection bulkConnection, String jobId, String batchId)
  {
    super(task.getBufferAllocator(), new SingleFileProvider(recordKeyList, bulkConnection, jobId, batchId));
  }

  @Override
  public void abort()
  {
  }

  @Override
  public TaskReport commit()
  {
    return Exec.newTaskReport();
  }

}
