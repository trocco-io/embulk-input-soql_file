package org.embulk.input.soql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
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

public class TestSoqlFilePluginWithIncremental {
    private static final ConfigMapperFactory CONFIG_MAPPER_FACTORY =
            ConfigMapperFactory.builder().addDefaultModules().build();
    private static final ConfigMapper CONFIG_MAPPER = CONFIG_MAPPER_FACTORY.createConfigMapper();

    private SoqlFilePlugin plugin;

    @Before
    public void setup() {
        plugin = new SoqlFilePlugin();
    }

    @Test
    public void testTransactionWithIncrementalAndLastRecord() {
        TaskReport report = CONFIG_MAPPER_FACTORY.newTaskReport();
        List<String> maxValues = new ArrayList<>();
        maxValues.add("999");
        report.set("last_record", maxValues);

        List<TaskReport> reports = new ArrayList<>();
        reports.add(report);

        ConfigDiff configDiff =
                plugin.transaction(
                        config(),
                        new FileInputPlugin.Control() {
                            @Override
                            public List<TaskReport> run(
                                    final TaskSource taskSource, final int taskCount) {
                                return reports;
                            }
                        });

        assertTrue(configDiff.has("last_record"));
        assertEquals(1, configDiff.get(List.class, "last_record").size());
        assertEquals("999", configDiff.get(List.class, "last_record").get(0));
    }

    @Test
    public void testResumeWithIncrementalAndLastRecord() {
        PluginTask task = CONFIG_MAPPER.map(config(), PluginTask.class);
        TaskReport report = CONFIG_MAPPER_FACTORY.newTaskReport();
        List<String> maxValues = new ArrayList<>();
        maxValues.add("999");
        report.set("last_record", maxValues);

        List<TaskReport> reports = new ArrayList<>();
        reports.add(report);

        ConfigDiff configDiff =
                plugin.resume(
                        task.toTaskSource(),
                        0,
                        new FileInputPlugin.Control() {
                            @Override
                            public List<TaskReport> run(
                                    final TaskSource taskSource, final int taskCount) {
                                return reports;
                            }
                        });

        assertTrue(configDiff.has("last_record"));
        assertEquals(1, configDiff.get(List.class, "last_record").size());
        assertEquals("999", configDiff.get(List.class, "last_record").get(0));
    }

    private ConfigSource config() {
        return CONFIG_MAPPER_FACTORY
                .newConfigSource()
                .set("object", "Account")
                .set("select", "Id,Name,Timestamp")
                .set("incremental", true)
                .set("incremental_columns", Arrays.asList("Id"));
    }
}
