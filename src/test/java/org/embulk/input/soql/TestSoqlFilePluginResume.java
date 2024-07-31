package org.embulk.input.soql;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.embulk.EmbulkTestRuntime;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;
import org.embulk.spi.FileInputPlugin;
import org.embulk.util.config.ConfigMapper;
import org.embulk.util.config.ConfigMapperFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestSoqlFilePluginResume {
    private static final ConfigMapperFactory CONFIG_MAPPER_FACTORY =
            ConfigMapperFactory.builder().addDefaultModules().build();

    @Rule public EmbulkTestRuntime runtime = new EmbulkTestRuntime();

    private SoqlFilePlugin plugin;

    @Before
    public void createResources() {
        plugin = new SoqlFilePlugin();
    }

    @Test
    public void testTransaction() {
        final ConfigDiff configDiff = plugin.transaction(config(), new TestControl());
        assertEquals(configDiff.get(String.class, "object"), "Account");
    }

    @Test
    public void testResume() {
        final ConfigMapper configMapper = CONFIG_MAPPER_FACTORY.createConfigMapper();
        final PluginTask task = configMapper.map(config(), PluginTask.class);
        final ConfigDiff configDiff = plugin.resume(task.toTaskSource(), 0, new TestControl());
        assertEquals(configDiff.get(String.class, "object"), "Account");
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
