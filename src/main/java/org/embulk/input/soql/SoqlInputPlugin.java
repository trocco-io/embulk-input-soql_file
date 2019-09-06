package org.embulk.input.soql;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.JsonArray;
import com.sforce.async.AsyncApiException;

import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigException;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;
import org.embulk.exec.ExecutionInterruptedException;
import org.embulk.spi.Exec;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.PageOutput;
import org.embulk.spi.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SoqlInputPlugin implements InputPlugin
{
    private final Logger logger =  LoggerFactory.getLogger(SoqlInputPlugin.class);

    @Override
    public ConfigDiff transaction(ConfigSource config, InputPlugin.Control control)
    {
        PluginTask task = config.loadConfig(PluginTask.class);
        Schema schema = task.getColumns().toSchema();
        return resume(task.dump(), schema, 1, control);
    }

    @Override
    public ConfigDiff resume(TaskSource taskSource, Schema schema, int taskCount, InputPlugin.Control control)
    {
        control.run(taskSource, schema, taskCount);
        return Exec.newConfigDiff();
    }

    @Override
    public void cleanup(TaskSource taskSource, Schema schema, int taskCount, List<TaskReport> successTaskReports)
    {
        String msg = "SoqlInputPlugin.cleanup method is not implemented yet";
        logger.error(msg);
        throw new UnsupportedOperationException(msg);
    }

    @Override
    public TaskReport run(TaskSource taskSource, Schema schema, int taskIndex, PageOutput output)
    {
        PluginTask pluginTask = taskSource.loadTask(PluginTask.class);
        try {
            ForceClient forceClient = new ForceClient(pluginTask);
            try (PageBuilder pageBuilder = new PageBuilder(Exec.getBufferAllocator(), schema, output)) {
                InputPluginUtils.addRunRecord(schema, pluginTask, forceClient, pageBuilder);
                pageBuilder.finish();
                return Exec.newTaskReport();
            }
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

    @Override
    public ConfigDiff guess(ConfigSource config)
    {
        try {
            PluginTask pluginTask = config.loadConfig(PluginTask.class);
            ForceClient forceClient = new ForceClient(pluginTask);
            String soql = SoqlUtils.soqlForGuess(pluginTask.getSoql());
            JsonArray jsonArray = forceClient.query(pluginTask.getObject(), soql);
            JsonNode columns = InputPluginUtils.createGuessColums(jsonArray);
            return Exec.newConfigDiff().set("columns", columns);
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
