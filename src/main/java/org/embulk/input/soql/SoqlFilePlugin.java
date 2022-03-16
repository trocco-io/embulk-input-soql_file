package org.embulk.input.soql;

import java.util.List;

import com.sforce.async.AsyncApiException;
import com.sforce.async.BatchInfo;
import com.sforce.async.BulkConnection;
import com.sforce.async.JobInfo;

import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigException;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;
import org.embulk.exec.ExecutionInterruptedException;
import org.embulk.spi.Exec;
import org.embulk.spi.FileInputPlugin;
import org.embulk.spi.TransactionalFileInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SoqlFilePlugin implements FileInputPlugin
{
    private final Logger logger = LoggerFactory.getLogger(SoqlFilePlugin.class);

    @Override
    public ConfigDiff transaction(ConfigSource config, FileInputPlugin.Control control)
    {
        PluginTask task = config.loadConfig(PluginTask.class);
        return resume(task.dump(), 1, control);
    }

    @Override
    public ConfigDiff resume(TaskSource taskSource, int taskCount, FileInputPlugin.Control control)
    {
        control.run(taskSource, taskCount);
        return Exec.newConfigDiff();
    }

    @Override
    public void cleanup(TaskSource taskSource, int taskCount, List<TaskReport> successTaskReports)
    {
    }

    @Override
    public TransactionalFileInput open(TaskSource taskSource, int taskIndex)
    {
        PluginTask pluginTask = taskSource.loadTask(PluginTask.class);
        try {
            ForceClient forceClient = new ForceClient(pluginTask);
            List<String> recordKeyList = forceClient.query(pluginTask);
            BulkConnection bulkConnection = forceClient.getBulkConnection();
            JobInfo jobInfo = forceClient.getJobInfo();
            BatchInfo batchInfo = forceClient.getBatchInfo();
            TransactionalFileInput input = new SoqlFileInput(pluginTask, recordKeyList, bulkConnection, jobInfo.getId(), batchInfo.getId());
            bulkConnection.closeJob(jobInfo.getId());
            return input;
        }
        catch (AsyncApiException e) {
            logger.error(e.getMessage(), e);
            throw new ConfigException(e);
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ExecutionInterruptedException(e);
        }
    }
}
