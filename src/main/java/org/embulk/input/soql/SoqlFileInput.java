package org.embulk.input.soql;

import com.sforce.async.BulkConnection;
import java.util.List;
import org.embulk.config.TaskReport;
import org.embulk.spi.Exec;
import org.embulk.spi.TransactionalFileInput;
import org.embulk.util.config.ConfigMapperFactory;
import org.embulk.util.file.InputStreamFileInput;

public class SoqlFileInput extends InputStreamFileInput implements TransactionalFileInput {
    private static final ConfigMapperFactory CONFIG_MAPPER_FACTORY =
            ConfigMapperFactory.builder().addDefaultModules().build();

    public SoqlFileInput(
            PluginTask task,
            List<String> recordKeyList,
            BulkConnection bulkConnection,
            String jobId,
            String batchId) {
        super(
                Exec.getBufferAllocator(),
                new SingleFileProvider(recordKeyList, bulkConnection, jobId, batchId));
    }

    @Override
    public void abort() {}

    @Override
    public TaskReport commit() {
        return CONFIG_MAPPER_FACTORY.newTaskReport();
    }
}
