package org.embulk.input.soql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TestCsvMaxValueFinderException {

    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testEmptyCsvFile() throws IOException {
        Path csv = createCsvFile("empty.csv", "");

        CsvMaxValueFinder finder = new CsvMaxValueFinder(Arrays.asList(csv), Arrays.asList("id"));

        List<String> maxValues = finder.findMaxValues();

        assertEquals(1, maxValues.size());
        assertNull(maxValues.get(0));
    }

    @Test
    public void testColumnNotFound() throws IOException {
        Path csv =
                createCsvFile(
                        "test1.csv",
                        "id,name,timestamp\n"
                                + "1,Alice,2024-08-21T10:15:30Z\n"
                                + "2,Bob,2024-08-22T11:30:00Z\n");
        CsvMaxValueFinder finder =
                new CsvMaxValueFinder(
                        Arrays.asList(csv), Arrays.asList("id", "nonexistent_column"));

        List<String> maxValues = finder.findMaxValues();

        // カラムが見つからない場合、その列の値はnullとする
        assertEquals(2, maxValues.size());
        assertEquals("2", maxValues.get(0));
        assertNull(maxValues.get(1));
    }

    @Test
    public void testInvalidDataFormat() throws IOException {
        Path csv =
                createCsvFile(
                        "invalid_data.csv",
                        "id,name,timestamp\n"
                                + "1,Alice,invalid_date_format\n"
                                + "2,Bob,2024-08-22T11:30:00Z\n");

        CsvMaxValueFinder finder =
                new CsvMaxValueFinder(Arrays.asList(csv), Arrays.asList("id", "timestamp"));

        List<String> maxValues = finder.findMaxValues();

        // 異なるデータ型が混在する場合、その列の値はnullとする
        assertEquals(2, maxValues.size());
        assertEquals("2", maxValues.get(0));
        assertNull(maxValues.get(1));
    }

    @Test
    public void testNoHeaderRow() throws IOException {
        Path csv =
                createCsvFile(
                        "no_header.csv",
                        "1,Alice,2024-08-21T10:15:30Z\n" + "2,Bob,2024-08-22T11:30:00Z\n");

        CsvMaxValueFinder finder = new CsvMaxValueFinder(Arrays.asList(csv), Arrays.asList("id"));

        List<String> maxValues = finder.findMaxValues();

        // ヘッダ行がない場合、結果はnullのリストになる
        assertEquals(1, maxValues.size());
        assertNull(maxValues.get(0));
    }

    private Path createCsvFile(String fileName, String content) throws IOException {
        File csvFile = folder.newFile(fileName);
        try (BufferedWriter writer = Files.newBufferedWriter(csvFile.toPath())) {
            writer.write(content);
        }
        return csvFile.toPath();
    }
}
