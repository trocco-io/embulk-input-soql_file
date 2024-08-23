package org.embulk.input.soql;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

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
        List<Boolean> invalidColumns = new ArrayList<>();
        for (int i = 0; i < targetColumnNames.size(); i++) {
            maxValues.add(null);
            invalidColumns.add(false);
        }

        CSVFormat csvFormat =
                CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build();
        try {
            for (Path csvFilePath : csvFilePaths) {
                CSVParser parser = CSVParser.parse(Files.newBufferedReader(csvFilePath), csvFormat);
                for (CSVRecord record : parser) {
                    for (int i = 0; i < targetColumnNames.size(); i++) {
                        // 無効化済みの列は無視する
                        if (invalidColumns.get(i)) {
                            continue;
                        }

                        String columnName = targetColumnNames.get(i);
                        // 指定された列が存在しない場合は無効化し以降無視する
                        if (!record.isSet(columnName)) {
                            invalidColumns.set(i, true);
                            continue;
                        }

                        String valueStr = record.get(columnName).trim();
                        Comparable<?> value = parseValue(valueStr);
                        Comparable<?> currentMax = maxValues.get(i);
                        // 異なるデータ型が混在する場合はその列を無効化し以降無視する
                        if (currentMax != null && !currentMax.getClass().equals(value.getClass())) {
                            invalidColumns.set(i, true);
                            continue;
                        }
                        // 現在の最大値より大きい場合は更新
                        if (currentMax == null
                                || (value != null
                                        && ((Comparable) value).compareTo(currentMax) > 0)) {
                            maxValues.set(i, value);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing CSV files: " + e.getMessage());
        }

        // 無効な列の結果はnullにする
        for (int i = 0; i < maxValues.size(); i++) {
            if (invalidColumns.get(i)) {
                maxValues.set(i, null);
            }
        }
        // Comparable<?> を String に変換して返却
        return maxValues.stream()
                .map(value -> value == null ? null : value.toString())
                .collect(Collectors.toList());
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
}
