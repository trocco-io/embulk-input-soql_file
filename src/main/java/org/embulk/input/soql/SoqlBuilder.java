package org.embulk.input.soql;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SoqlBuilder {
    private String select;
    private String object;
    private String where;
    private Integer limit;
    private List<String> incrementalColumns;
    private List<String> lastRecords;

    public SoqlBuilder(
            String select,
            String object,
            Optional<String> where,
            Optional<Integer> limit,
            List<String> incrementalColumns,
            Optional<List<String>> lastRecords) {
        this.select = select;
        this.object = object;
        this.where = where.orElse(null);
        this.limit = limit.orElse(null);
        this.incrementalColumns = Objects.requireNonNull(incrementalColumns);
        this.lastRecords = lastRecords.orElse(null);
    }

    public String build() {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT " + select);
        sb.append(" FROM " + object);
        if (where != null) {
            sb.append(" WHERE " + where);
        }
        // 差分転送のための WHERE 句を生成する:
        //
        //   WHERE
        //     ((incremental_columns[0] > last_record[0])
        //      OR (incremental_columns[0] = last_record[0] AND incremental_columns[1] >
        // last_record[1])
        //      OR (incremental_columns[0] = last_record[0] AND incremental_columns[1] =
        // last_record[1] AND incremental_columns[2] > last_record[2])
        //      OR (...))
        if (incrementalColumns.size() > 0 && lastRecords != null && lastRecords.size() > 0) {
            if (where != null) {
                sb.append(" AND ");
            } else {
                sb.append(" WHERE ");
            }
            sb.append("(");

            for (int i = 0; i < incrementalColumns.size(); i++) {
                if (i > 0) {
                    sb.append(" OR ");
                }
                if (incrementalColumns.size() > 1) {
                    sb.append("(");
                }
                for (int j = 0; j < i; j++) {
                    sb.append(
                            incrementalColumns.get(j)
                                    + " = "
                                    + escape(lastRecords.get(j))
                                    + " AND ");
                }
                sb.append(incrementalColumns.get(i) + " > " + escape(lastRecords.get(i)));
                if (incrementalColumns.size() > 1) {
                    sb.append(")");
                }
            }
            sb.append(")");
        }
        // 差分転送のための ORDER BY 句を生成する:
        //
        //   ORDER BY incremental_columns[0] ASC, incremental_columns[1] ASC, ...
        if (incrementalColumns.size() > 0) {
            sb.append(" ORDER BY ");
            for (int i = 0; i < incrementalColumns.size(); i++) {
                sb.append(incrementalColumns.get(i) + " ASC");
                if (i != incrementalColumns.size() - 1) {
                    sb.append(", ");
                }
            }
        }
        if (limit != null) {
            sb.append(" LIMIT " + limit);
        }

        return sb.toString();
    }

    private String escape(String value) {
        if (value == null) {
            return "NULL";
        }
        // SOQL では数値型、timestamp, date 型の値をクォートするとエラーになる
        if (isNumeric(value) || isTimestamp(value) || isDate(value)) {
            return value;
        }
        return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    private boolean isNumeric(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isTimestamp(String value) {
        try {
            ZonedDateTime.parse(value, DateTimeFormatter.ISO_ZONED_DATE_TIME);
            return true;
        } catch (DateTimeParseException e) {
            try {
                Instant.parse(value);
                return true;
            } catch (DateTimeParseException e2) {
                return false;
            }
        }
    }

    private boolean isDate(String value) {
        try {
            LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
