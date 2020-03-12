package org.embulk.input.soql;

import java.io.InputStream;
import java.util.List;

import org.embulk.config.TaskReport;
import org.embulk.spi.Exec;
import org.embulk.spi.TransactionalFileInput;
import org.embulk.spi.util.InputStreamFileInput;

public class SoqlFileInput extends InputStreamFileInput implements TransactionalFileInput
{

  public SoqlFileInput(PluginTask task, List<InputStream> inputStreams)
  {
    super(task.getBufferAllocator(), new SingleFileProvider(inputStreams));
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
