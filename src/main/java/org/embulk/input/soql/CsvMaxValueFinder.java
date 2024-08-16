package org.embulk.input.soql;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CsvMaxValueFinder {
    private List<Path> csvFilePaths;
    private List<String> targetColumnNames;

    public CsvMaxValueFinder(List<Path> csvFilePaths, List<String> targetColumnNames) {
        this.csvFilePaths = csvFilePaths;
        this.targetColumnNames = targetColumnNames;
    }

    @SuppressWarnings("unchecked")
    public List<String> findMaxValues() {
        List<Comparable<?>> maxValues = new ArrayList<>();
        for (int i = 0; i < targetColumnNames.size(); i++) {
            maxValues.add(null);
        }

        try {
            for (Path csvPath : csvFilePaths) {
                try (InputStream inputStream = Files.newInputStream(csvPath);
                        BufferedReader reader =
                                new BufferedReader(new InputStreamReader(inputStream))) {

                    String headerLine = reader.readLine(); // 1行目はヘッダ行
                    if (headerLine == null) {
                        continue; // ファイルが空なら次のファイルへ
                    }

                    String[] headers = parseCsvLine(headerLine);
                    for (String columnName : targetColumnNames) {
                        if (!isColumnPresent(headers, columnName)) {
                            System.err.println("Column name not found: " + columnName);
                            continue;
                        }
                    }

                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] values = parseCsvLine(line);

                        for (int i = 0; i < targetColumnNames.size(); i++) {
                            String columnName = targetColumnNames.get(i);
                            int targetColumnIndex = findColumnIndex(headers, columnName);
                            if (targetColumnIndex >= 0 && values.length > targetColumnIndex) {
                                Comparable<?> value = parseValue(values[targetColumnIndex].trim());
                                Comparable<?> currentMax = maxValues.get(i);
                                if (currentMax == null
                                        || (value != null
                                                && ((Comparable) value).compareTo(currentMax)
                                                        > 0)) {
                                    maxValues.set(i, value);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing CSV files: " + e.getMessage());
        }

        return maxValues.stream().map(Object::toString).collect(Collectors.toList());
    }

    private boolean isColumnPresent(String[] headers, String columnName) {
        for (String header : headers) {
            if (header.trim().equals(columnName)) {
                return true;
            }
        }
        return false;
    }

    private int findColumnIndex(String[] headers, String columnName) {
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].trim().equals(columnName)) {
                return i;
            }
        }
        return -1;
    }

    private Comparable<?> parseValue(String value) {
        try {
            // 数値として解釈できる場合
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            // 無視して次の解釈へ
        }

        try {
            // タイムスタンプとして解釈できる場合
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            // 無視して次の解釈へ
        }

        // どちらでもなければ文字列として扱う
        return value;
    }

    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (char ch : line.toCharArray()) {
            if (ch == '"') {
                inQuotes = !inQuotes; // クオートの開始/終了を反転
            } else if (ch == ',' && !inQuotes) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }

        result.add(current.toString()); // 最後のフィールドを追加
        return result.toArray(new String[0]);
    }
}
