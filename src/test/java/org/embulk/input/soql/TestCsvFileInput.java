package org.embulk.input.soql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.embulk.EmbulkTestRuntime;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskReport;
import org.embulk.spi.DataException;
import org.embulk.util.config.ConfigMapper;
import org.embulk.util.config.ConfigMapperFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestCsvFileInput {
    private static final ConfigMapperFactory CONFIG_MAPPER_FACTORY =
            ConfigMapperFactory.builder().addDefaultModules().build();
    private static final ConfigMapper CONFIG_MAPPER = CONFIG_MAPPER_FACTORY.createConfigMapper();

    @Rule public EmbulkTestRuntime runtime = new EmbulkTestRuntime();

    private CsvFileInput csvFileInput;
    private Path csvFilePath;

    @Before
    public void setup() throws IOException {
        List<Path> csvFilePaths = new ArrayList<>();
        csvFilePath = Files.createTempFile("test", ".csv");
        csvFilePaths.add(csvFilePath);

        ConfigSource config =
                runtime.getExec()
                        .newConfigSource()
                        .set("object", "Account")
                        .set("select", "Id,Name,Timestamp")
                        .set("incremental", true)
                        .set("incremental_columns", Arrays.asList("Id", "Timestamp"));
        PluginTask task = CONFIG_MAPPER.map(config, PluginTask.class);

        csvFileInput = new CsvFileInput(task, csvFilePaths);
    }

    @Test
    public void testCommitWithRecords() throws IOException {
        String csv =
                "Id,Name,Timestamp\n"
                        + "1,John,2024-08-29 12:34:56\n"
                        + "2,Jane,2024-08-30 14:56:00\n";
        writeCsvDataToFile(csv, csvFilePath);

        TaskReport report = csvFileInput.commit();
        assertEquals(
                Arrays.asList("2", "2024-08-30 14:56:00"), report.get(List.class, "last_record"));
    }

    @Test
    public void testCommitWithNoRecords() throws IOException {
        String csv = "Id,Name,Timestamp\n";
        writeCsvDataToFile(csv, csvFilePath);

        TaskReport report = csvFileInput.commit();
        assertTrue(report.isEmpty());
    }

    @Test
    public void testCommitWithEmptyCsv() throws IOException {
        writeCsvDataToFile("", csvFilePath);

        TaskReport report = csvFileInput.commit();
        assertTrue(report.isEmpty());
    }

    @Test
    public void testCommitWithInvalidRecords() throws IOException {
        String csv = "Id,Name,Timestamp\n" + "1,John,\n";
        writeCsvDataToFile(csv, csvFilePath);

        try {
            csvFileInput.commit();
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof DataException);
        }
    }

    private void writeCsvDataToFile(String csv, Path path) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write(csv);
        }
    }
}
