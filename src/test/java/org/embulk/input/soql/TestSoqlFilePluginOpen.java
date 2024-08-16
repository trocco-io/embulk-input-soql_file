package org.embulk.input.soql;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

import com.sforce.async.AsyncApiException;
import com.sforce.async.BatchInfo;
import com.sforce.async.BulkConnection;
import com.sforce.async.JobInfo;
import com.sforce.ws.ConnectionException;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import org.embulk.EmbulkTestRuntime;
import org.embulk.config.ConfigSource;
import org.embulk.spi.TransactionalFileInput;
import org.embulk.util.config.ConfigMapper;
import org.embulk.util.config.ConfigMapperFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

public class TestSoqlFilePluginOpen {
    private static final ConfigMapperFactory CONFIG_MAPPER_FACTORY =
            ConfigMapperFactory.builder().addDefaultModules().build();
    private static final ConfigMapper CONFIG_MAPPER = CONFIG_MAPPER_FACTORY.createConfigMapper();

    @Rule public EmbulkTestRuntime runtime = new EmbulkTestRuntime();

    private ForceClient forceClient;
    private BulkConnection bulkConnection;
    private JobInfo jobInfo;
    private BatchInfo batchInfo;
    private SoqlFilePlugin plugin;

    @Before
    public void setup() {
        forceClient = mock(ForceClient.class);
        bulkConnection = mock(BulkConnection.class);
        jobInfo = mock(JobInfo.class);
        batchInfo = mock(BatchInfo.class);
        when(forceClient.getBulkConnection()).thenReturn(bulkConnection);
        when(forceClient.getJobInfo()).thenReturn(jobInfo);
        when(forceClient.getBatchInfo()).thenReturn(batchInfo);

        plugin =
                new SoqlFilePlugin() {
                    @Override
                    protected ForceClient createForceClient(PluginTask task)
                            throws AsyncApiException, ConnectionException {
                        return forceClient;
                    }
                };
    }

    @Test
    public void testOpen() throws Exception {
        final PluginTask pluginTask = CONFIG_MAPPER.map(config(), PluginTask.class);
        final String soql = pluginTask.getSoql().get();

        when(forceClient.query(any(PluginTask.class), anyString()))
                .thenReturn(Arrays.asList("result1", "result2"));
        when(jobInfo.getId()).thenReturn("jobId");
        when(batchInfo.getId()).thenReturn("batchId");
        String csv1 = "Id,Name\n" + "1,Alice\n" + "2,Bob\n" + "3,Charlie\n";
        String csv2 = "Id,Name\n" + "4,Dave\n" + "5,Frank\n" + "6,Eve\n";
        when(bulkConnection.getQueryResultStream("jobId", "batchId", "result1"))
                .thenReturn(new ByteArrayInputStream(csv1.getBytes()));
        when(bulkConnection.getQueryResultStream("jobId", "batchId", "result2"))
                .thenReturn(new ByteArrayInputStream(csv2.getBytes()));

        TransactionalFileInput result = plugin.open(pluginTask.toTaskSource(), 0);
        assertNotNull(result);
        Mockito.verify(bulkConnection).closeJob("jobId");
    }

    private ConfigSource config() {
        return runtime.getExec()
                .newConfigSource()
                .set("object", "Account")
                .set("soql", "SELECT Id,Name FROM Account");
    }
}
