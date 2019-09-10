package org.embulk.input.soql;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.embulk.exec.ExecutionInterruptedException;
import org.embulk.spi.Column;
import org.embulk.spi.ColumnVisitor;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.json.JsonParser;
import org.embulk.spi.time.Timestamp;
import org.embulk.spi.time.TimestampParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SoqlColumnVisitor
 */
public class SoqlColumnVisitor implements ColumnVisitor
{
    private final Logger logger =  LoggerFactory.getLogger(SoqlColumnVisitor.class);

    private JsonObject record;
    private PageBuilder pageBuilder;
    private PluginTask pluginTask;

    public static final String DEFAULT_TIMESTAMP_PATTERN = "%Y-%m-%dT%H:%M:%S.%L%z";

    public SoqlColumnVisitor(JsonObject record, PageBuilder pageBuilder, PluginTask pluginTask)
    {
        this.record = record;
        this.pageBuilder = pageBuilder;
        this.pluginTask = pluginTask;
    }

    @Override
    public void booleanColumn(Column column)
    {
        try {
            JsonElement data = record.get(column.getName());
            if (data.isJsonNull()) {
                pageBuilder.setNull(column);
            }
            else if (data.isJsonPrimitive()) {
                pageBuilder.setBoolean(column, data.getAsBoolean());
            }
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ExecutionInterruptedException(e);
        }
    }

    @Override
    public void longColumn(Column column)
    {
        try {
            JsonElement data = record.get(column.getName());
            if (data.isJsonNull()) {
                pageBuilder.setNull(column);
            }
            else if (data.isJsonPrimitive()) {
                pageBuilder.setLong(column, data.getAsLong());
            }
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ExecutionInterruptedException(e);
        }
    }

    @Override
    public void doubleColumn(Column column)
    {
        try {
            JsonElement data = record.get(column.getName());
            if (data.isJsonNull()) {
                pageBuilder.setNull(column);
            }
            else if (data.isJsonPrimitive()) {
                pageBuilder.setDouble(column, data.getAsDouble());
            }
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ExecutionInterruptedException(e);
        }
    }

    @Override
    public void stringColumn(Column column)
    {
        try {
            JsonElement data = record.get(column.getName());
            if (data.isJsonNull()) {
                pageBuilder.setNull(column);
            }
            else if (data.isJsonPrimitive()) {
                pageBuilder.setString(column, data.getAsString());
            }
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ExecutionInterruptedException(e);
        }
    }

    @Override
    public void timestampColumn(Column column)
    {
        try {
            JsonElement data = record.get(column.getName());
            if (data.isJsonNull()) {
                pageBuilder.setNull(column);
            }
            else {
                String format = pluginTask.getColumns().getColumn(column.getIndex()).getConfigSource().getObjectNode().get("format").asText();
                TimestampParser parser = TimestampParser.of(format, "JST");
                Timestamp value = parser.parse(data.getAsString());
                pageBuilder.setTimestamp(column, value);
            }
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ExecutionInterruptedException(e);
        }
    }

    @Override
    public void jsonColumn(Column column)
    {
        try {
            JsonElement data = record.get(column.getName());
            if (data.isJsonNull() || data.isJsonPrimitive()) {
                pageBuilder.setNull(column);
            }
            else {
                pageBuilder.setJson(column, new JsonParser().parse(data.toString()));
            }
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new ExecutionInterruptedException(e);
        }
    }
}
