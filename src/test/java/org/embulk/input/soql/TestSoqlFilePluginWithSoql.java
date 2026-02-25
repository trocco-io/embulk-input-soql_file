package org.embulk.input.soql;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;
import org.embulk.spi.FileInputPlugin;
import org.embulk.util.config.ConfigMapper;
import org.embulk.util.config.ConfigMapperFactory;
import org.junit.Before;
import org.junit.Test;

public class TestSoqlFilePluginWithSoql {
    private static final ConfigMapperFactory CONFIG_MAPPER_FACTORY =
            ConfigMapperFactory.builder().addDefaultModules().build();
    private static final ConfigMapper CONFIG_MAPPER = CONFIG_MAPPER_FACTORY.createConfigMapper();

    private SoqlFilePlugin plugin;

    @Before
    public void setup() {
        plugin = new SoqlFilePlugin();
    }

    @Test
    public void testTransaction() {
        ConfigDiff configDiff = plugin.transaction(config(), new TestControl());
        assertTrue(configDiff.isEmpty());
    }

    @Test
    public void testResume() {
        PluginTask task = CONFIG_MAPPER.map(config(), PluginTask.class);
        ConfigDiff configDiff = plugin.resume(task.toTaskSource(), 0, new TestControl());
        assertTrue(configDiff.isEmpty());
    }

    private ConfigSource config() {
        return CONFIG_MAPPER_FACTORY
                .newConfigSource()
                .set("object", "Account")
                .set("soql", "SELECT Id,Name FROM Account");
    }

    private static class TestControl implements FileInputPlugin.Control {
        @Override
        public List<TaskReport> run(final TaskSource taskSource, final int taskCount) {
            return new ArrayList<>();
        }
    }
}
