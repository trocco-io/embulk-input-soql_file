package org.embulk.input.soql;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.embulk.config.TaskReport;
import org.embulk.spi.DataException;
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

        if (task.getIncremental()) {
            try {
                Path csvFilePath = csvFilePaths.get(csvFilePaths.size() - 1);
                List<String> lastRecords =
                        getLastRecords(csvFilePath, task.getIncrementalColumns());
                if (!lastRecords.isEmpty()) {
                    report.set("last_record", lastRecords);
                }
            } catch (IOException | DataException e) {
                throw new RuntimeException(e);
            }
        }

        return report;
    }

    protected List<String> getLastRecords(Path csvFilePath, List<String> columns)
            throws IOException, DataException {
        List<String> values = new ArrayList<>();
        CSVFormat format =
                CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build();

        try (BufferedReader reader = Files.newBufferedReader(csvFilePath);
                CSVParser parser = new CSVParser(reader, format)) {
            CSVRecord lastRecord = null;
            for (CSVRecord record : parser) {
                lastRecord = record;
            }
            if (lastRecord != null) {
                for (String column : columns) {
                    String value = lastRecord.get(column);
                    if (value == null || value.isEmpty()) {
                        throw new DataException(
                                "incremental_columns can't include null values but the last row is null at column: "
                                        + column);
                    }
                    values.add(value);
                }
            }
        }

        return values;
    }
}
