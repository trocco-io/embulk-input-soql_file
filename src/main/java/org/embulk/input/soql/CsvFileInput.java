package org.embulk.input.soql;

import java.nio.file.Path;
import java.util.List;
import org.embulk.config.TaskReport;
import org.embulk.spi.Exec;
import org.embulk.spi.TransactionalFileInput;
import org.embulk.util.config.ConfigMapperFactory;
import org.embulk.util.file.InputStreamFileInput;

public class CsvFileInput extends InputStreamFileInput implements TransactionalFileInput {
    private static final ConfigMapperFactory CONFIG_MAPPER_FACTORY =
            ConfigMapperFactory.builder().addDefaultModules().build();

    private PluginTask task;
    private List<Path> csvFilePaths;

    public CsvFileInput(PluginTask task, List<Path> csvFilePaths) {
        super(Exec.getBufferAllocator(), new CsvFileProvider(csvFilePaths));
        this.task = task;
        this.csvFilePaths = csvFilePaths;
    }

    @Override
    public void abort() {}

    @Override
    public TaskReport commit() {
        TaskReport report = CONFIG_MAPPER_FACTORY.newTaskReport();

        // CSV から最大値をとりだす
        if (task.getIncremental()) {
            CsvMaxValueFinder finder =
                    new CsvMaxValueFinder(csvFilePaths, task.getIncrementalColumns());
            List<String> maxValues = finder.findMaxValues();
            report.set("last_record", maxValues);
        }

        return report;
    }
}
