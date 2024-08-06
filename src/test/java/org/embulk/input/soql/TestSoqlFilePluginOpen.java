package org.embulk.input.soql;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sforce.async.BatchInfo;
import com.sforce.async.BulkConnection;
import com.sforce.async.JobInfo;
import java.util.Arrays;
import org.embulk.EmbulkTestRuntime;
import org.embulk.config.ConfigSource;
import org.embulk.spi.TransactionalFileInput;
import org.embulk.util.config.ConfigMapper;
import org.embulk.util.config.ConfigMapperFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TestSoqlFilePluginOpen {
    private static final ConfigMapperFactory CONFIG_MAPPER_FACTORY =
            ConfigMapperFactory.builder().addDefaultModules().build();

    @Rule public EmbulkTestRuntime runtime = new EmbulkTestRuntime();

    private ForceClient forceClient;
    private BulkConnection bulkConnection;
    private JobInfo jobInfo;
    private BatchInfo batchInfo;
    private SoqlFilePlugin plugin;

    @Before
    public void createResources() {
        forceClient = mock(ForceClient.class);
        bulkConnection = mock(BulkConnection.class);
        jobInfo = mock(JobInfo.class);
        batchInfo = mock(BatchInfo.class);

        plugin = new SoqlFilePlugin();
    }

    @Test
    public void testOpen() throws Exception {
        final ConfigMapper configMapper = CONFIG_MAPPER_FACTORY.createConfigMapper();
        final PluginTask pluginTask = configMapper.map(config(), PluginTask.class);

        when(forceClient.query(pluginTask)).thenReturn(Arrays.asList("record1", "record2"));
        when(forceClient.getBulkConnection()).thenReturn(bulkConnection);
        when(forceClient.getJobInfo()).thenReturn(jobInfo);
        when(forceClient.getBatchInfo()).thenReturn(batchInfo);
        when(jobInfo.getId()).thenReturn("jobId");
        when(batchInfo.getId()).thenReturn("batchId");

        TransactionalFileInput result = plugin.open(pluginTask.toTaskSource(), 0);

        assertNotNull(result);
        Mockito.verify(bulkConnection).closeJob("jobId");
    }

    private ConfigSource config() {
        return CONFIG_MAPPER_FACTORY
                .newConfigSource()
                .set("object", "Account")
                .set("soql", "SELECT Id,Name FROM Account");
    }
}
