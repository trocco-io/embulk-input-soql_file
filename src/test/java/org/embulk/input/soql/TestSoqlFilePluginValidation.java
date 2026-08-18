package org.embulk.input.soql;

import java.util.ArrayList;
import java.util.List;
import org.embulk.config.ConfigException;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;
import org.embulk.spi.FileInputPlugin;
import org.embulk.util.config.ConfigMapper;
import org.embulk.util.config.ConfigMapperFactory;
import org.junit.Before;
import org.junit.Test;

public class TestSoqlFilePluginValidation {
    private static final ConfigMapperFactory CONFIG_MAPPER_FACTORY =
            ConfigMapperFactory.builder().addDefaultModules().build();
    private static final ConfigMapper CONFIG_MAPPER = CONFIG_MAPPER_FACTORY.createConfigMapper();

    private SoqlFilePlugin plugin;

    @Before
    public void setup() {
        plugin = new SoqlFilePlugin();
    }

    @Test(expected = ConfigException.class)
    public void testTransactionWithoutSoqlAndIncrementalFalse() {
        ConfigSource config =
                CONFIG_MAPPER_FACTORY
                        .newConfigSource()
                        .set("auth_method", "oauth")
                        .set("object", "Account");
        plugin.transaction(config, new NoOpControl());
    }

    @Test(expected = ConfigException.class)
    public void testTransactionWithBothSoqlAndSelect() {
        ConfigSource config =
                CONFIG_MAPPER_FACTORY
                        .newConfigSource()
                        .set("auth_method", "oauth")
                        .set("object", "Account")
                        .set("soql", "SELECT Id,Name FROM Account")
                        .set("select", "Id,Name");
        plugin.transaction(config, new NoOpControl());
    }

    @Test(expected = ConfigException.class)
    public void testTransactionWithSoqlAndIncremental() {
        ConfigSource config =
                CONFIG_MAPPER_FACTORY
                        .newConfigSource()
                        .set("auth_method", "oauth")
                        .set("object", "Account")
                        .set("soql", "SELECT Id,Name FROM Account")
                        .set("incremental", true);
        plugin.transaction(config, new NoOpControl());
    }

    @Test(expected = ConfigException.class)
    public void testTransactionWithIncrementalAndEmptyIncrementalColumns() {
        ConfigSource config =
                CONFIG_MAPPER_FACTORY
                        .newConfigSource()
                        .set("auth_method", "oauth")
                        .set("object", "Account")
                        .set("incremental", true);
        plugin.transaction(config, new NoOpControl());
    }

    @Test(expected = ConfigException.class)
    public void testResumeWithoutSoqlAndIncrementalFalse() {
        ConfigSource config =
                CONFIG_MAPPER_FACTORY
                        .newConfigSource()
                        .set("auth_method", "oauth")
                        .set("object", "Account");
        PluginTask task = CONFIG_MAPPER.map(config, PluginTask.class);
        plugin.resume(task.toTaskSource(), 0, new NoOpControl());
    }

    @Test(expected = ConfigException.class)
    public void testResumeWithBothSoqlAndSelect() {
        ConfigSource config =
                CONFIG_MAPPER_FACTORY
                        .newConfigSource()
                        .set("auth_method", "oauth")
                        .set("object", "Account")
                        .set("soql", "SELECT Id,Name FROM Account")
                        .set("select", "Id,Name");
        PluginTask task = CONFIG_MAPPER.map(config, PluginTask.class);
        plugin.resume(task.toTaskSource(), 0, new NoOpControl());
    }

    @Test(expected = ConfigException.class)
    public void testTransactionWithUserPasswordAndApiVersion65() {
        ConfigSource config =
                CONFIG_MAPPER_FACTORY
                        .newConfigSource()
                        .set("auth_method", "user_password")
                        .set("api_version", "65.0")
                        .set("object", "Account")
                        .set("soql", "SELECT Id,Name FROM Account");
        plugin.transaction(config, new NoOpControl());
    }

    @Test(expected = ConfigException.class)
    public void testTransactionWithUserPasswordAndDefaultApiVersion() {
        // The default api_version (67.0) is not usable with user_password auth.
        ConfigSource config =
                CONFIG_MAPPER_FACTORY
                        .newConfigSource()
                        .set("auth_method", "user_password")
                        .set("object", "Account")
                        .set("soql", "SELECT Id,Name FROM Account");
        plugin.transaction(config, new NoOpControl());
    }

    @Test
    public void testTransactionWithUserPasswordAndApiVersion64() {
        ConfigSource config =
                CONFIG_MAPPER_FACTORY
                        .newConfigSource()
                        .set("auth_method", "user_password")
                        .set("api_version", "64.0")
                        .set("object", "Account")
                        .set("soql", "SELECT Id,Name FROM Account");
        plugin.transaction(config, new NoOpControl());
    }

    @Test
    public void testTransactionWithOauthAndApiVersion67() {
        ConfigSource config =
                CONFIG_MAPPER_FACTORY
                        .newConfigSource()
                        .set("auth_method", "oauth")
                        .set("api_version", "67.0")
                        .set("object", "Account")
                        .set("soql", "SELECT Id,Name FROM Account");
        plugin.transaction(config, new NoOpControl());
    }

    private static class NoOpControl implements FileInputPlugin.Control {
        @Override
        public List<TaskReport> run(final TaskSource taskSource, final int taskCount) {
            return new ArrayList<>();
        }
    }
}
