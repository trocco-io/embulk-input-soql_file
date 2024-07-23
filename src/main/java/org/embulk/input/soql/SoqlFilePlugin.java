package org.embulk.input.soql;

import com.sforce.async.AsyncApiException;
import com.sforce.async.BatchInfo;
import com.sforce.async.BulkConnection;
import com.sforce.async.JobInfo;
import java.util.List;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigException;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;
import org.embulk.spi.FileInputPlugin;
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

    @Override
    public ConfigDiff transaction(ConfigSource config, FileInputPlugin.Control control) {
        final ConfigMapper configMapper = CONFIG_MAPPER_FACTORY.createConfigMapper();
        final PluginTask pluginTask = configMapper.map(config, PluginTask.class);

        return resume(pluginTask.toTaskSource(), 1, control);
    }

    @Override
    public ConfigDiff resume(
            TaskSource taskSource, int taskCount, FileInputPlugin.Control control) {
        control.run(taskSource, taskCount);
        return CONFIG_MAPPER_FACTORY.newConfigDiff();
    }

    @Override
    public void cleanup(
            TaskSource taskSource, int taskCount, List<TaskReport> successTaskReports) {}

    @Override
    public TransactionalFileInput open(TaskSource taskSource, int taskIndex) {
        final TaskMapper taskMapper = CONFIG_MAPPER_FACTORY.createTaskMapper();
        final PluginTask pluginTask = taskMapper.map(taskSource, PluginTask.class);

        try {
            ForceClient forceClient = new ForceClient(pluginTask);
            List<String> recordKeyList = forceClient.query(pluginTask);
            BulkConnection bulkConnection = forceClient.getBulkConnection();
            JobInfo jobInfo = forceClient.getJobInfo();
            BatchInfo batchInfo = forceClient.getBatchInfo();
            TransactionalFileInput input =
                    new SoqlFileInput(
                            pluginTask,
                            recordKeyList,
                            bulkConnection,
                            jobInfo.getId(),
                            batchInfo.getId());
            bulkConnection.closeJob(jobInfo.getId());
            return input;
        } catch (AsyncApiException e) {
            logger.error(e.getMessage(), e);
            throw new ConfigException(e);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
