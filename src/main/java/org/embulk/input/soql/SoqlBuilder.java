package org.embulk.input.soql;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SoqlBuilder {
    private String select;
    private String object;
    private String where;
    private List<String> incrementalColumns;
    private List<String> lastRecords;

    public SoqlBuilder(String select, String object) {
        this.select = select;
        this.object = object;
        this.incrementalColumns = new ArrayList<>();
    }

    public SoqlBuilder(String select, String object, Optional<String> where) {
        this(select, object);
        this.where = where.orElse(null);
    }

    public SoqlBuilder(
            String select,
            String object,
            Optional<String> where,
            List<String> incrementalColumns,
            Optional<List<String>> lastRecords) {
        this(select, object, where);
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
        //     (incremental_columns[0] > last_record[0]
        //      AND incremental_columns[1] > last_record[1]
        //      AND ...)
        if (incrementalColumns.size() > 0 && lastRecords != null && lastRecords.size() > 0) {
            if (where != null) {
                sb.append(" AND ");
            } else {
                sb.append(" WHERE ");
            }
            sb.append("(");
            for (int i = 0; i < incrementalColumns.size(); i++) {
                sb.append(incrementalColumns.get(i) + " > " + escape(lastRecords.get(i)));
                if (i != incrementalColumns.size() - 1) {
                    sb.append(" AND ");
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

        return sb.toString();
    }

    private String escape(String value) {
        if (value == null) {
            return "NULL";
        }
        return "'" + value.replace("'", "\\'") + "'";
    }
}
