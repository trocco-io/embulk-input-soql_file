package org.embulk.input.soql;

import static org.junit.Assert.assertEquals;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TestCsvMaxValueFinder {

    @Rule public TemporaryFolder folder = new TemporaryFolder();

    private Path csvFilePath1;
    private Path csvFilePath2;

    @Before
    public void setup() throws IOException {
        // テスト用のCSVファイルを準備
        csvFilePath1 =
                createCsvFile(
                        "test1.csv",
                        "id,name,timestamp\n"
                                + "1,Alice,2024-08-21T10:15:30Z\n"
                                + "2,Bob,2024-08-22T11:30:00Z\n"
                                + "3,Charlie,2024-08-20T09:45:00Z\n");

        csvFilePath2 =
                createCsvFile(
                        "test2.csv",
                        "id,name,timestamp\n"
                                + "4,Dave,2024-08-23T12:00:00Z\n"
                                + "5,Frank,2024-08-24T13:15:00Z\n"
                                + "6,Eve,2024-08-19T08:00:00Z\n");
    }

    @Test
    public void testFindMaxValuesWithNumber() {
        CsvMaxValueFinder finder =
                new CsvMaxValueFinder(
                        Arrays.asList(csvFilePath1, csvFilePath2), Arrays.asList("id"));

        List<String> maxValues = finder.findMaxValues();

        assertEquals(1, maxValues.size());
        assertEquals("6", maxValues.get(0));
    }

    @Test
    public void testFindMaxValuesWithString() {
        CsvMaxValueFinder finder =
                new CsvMaxValueFinder(
                        Arrays.asList(csvFilePath1, csvFilePath2), Arrays.asList("name"));

        List<String> maxValues = finder.findMaxValues();

        assertEquals(1, maxValues.size());
        assertEquals("Frank", maxValues.get(0));
    }

    @Test
    public void testFindMaxValuesWithTimestamp() {
        CsvMaxValueFinder finder =
                new CsvMaxValueFinder(
                        Arrays.asList(csvFilePath1, csvFilePath2), Arrays.asList("timestamp"));

        List<String> maxValues = finder.findMaxValues();

        assertEquals(1, maxValues.size());
        assertEquals("2024-08-24T13:15:00Z", maxValues.get(0));
    }

    @Test
    public void testFindMaxValuesWithMultipleColumns() {
        CsvMaxValueFinder finder =
                new CsvMaxValueFinder(
                        Arrays.asList(csvFilePath1, csvFilePath2),
                        Arrays.asList("id", "timestamp"));

        List<String> maxValues = finder.findMaxValues();

        assertEquals(2, maxValues.size());
        assertEquals("6", maxValues.get(0));
        assertEquals("2024-08-24T13:15:00Z", maxValues.get(1));
    }

    private Path createCsvFile(String fileName, String content) throws IOException {
        File csvFile = folder.newFile(fileName);
        try (BufferedWriter writer = Files.newBufferedWriter(csvFile.toPath())) {
            writer.write(content);
        }
        return csvFile.toPath();
    }
}
