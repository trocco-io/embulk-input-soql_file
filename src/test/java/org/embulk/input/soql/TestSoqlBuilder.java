package org.embulk.input.soql;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.Test;

public class TestSoqlBuilder {
    @Test
    public void testBuild() {
        String select = "Id, Name";
        String object = "Account";
        SoqlBuilder soqlBuilder = new SoqlBuilder(select, object);
        String soql = soqlBuilder.build();
        assertEquals(soql, "SELECT Id, Name FROM Account");
    }

    @Test
    public void testBuildWithWhere() {
        String select = "Id, Name";
        String object = "Account";
        Optional<String> where = Optional.of("Name != 'John Doe'");
        SoqlBuilder soqlBuilder = new SoqlBuilder(select, object, where);
        String soql = soqlBuilder.build();
        assertEquals(soql, "SELECT Id, Name FROM Account WHERE Name != 'John Doe'");
    }

    @Test
    public void testBuildWithIncrementalColumns() {
        String select = "Id, Name";
        String object = "Account";
        Optional<String> where = Optional.empty();
        List<String> incrementalColumns = Arrays.asList("Id");
        Optional<List<String>> lastRecords = Optional.empty();
        SoqlBuilder soqlBuilder =
                new SoqlBuilder(select, object, where, incrementalColumns, lastRecords);
        String soql = soqlBuilder.build();
        assertEquals(soql, "SELECT Id, Name FROM Account ORDER BY Id ASC");
    }

    @Test
    public void testBuildWithWhereAndIncrementalColumns() {
        String select = "Id, Name";
        String object = "Account";
        Optional<String> where = Optional.of("Name != 'John Doe'");
        List<String> incrementalColumns = Arrays.asList("Id");
        Optional<List<String>> lastRecords = Optional.empty();
        SoqlBuilder soqlBuilder =
                new SoqlBuilder(select, object, where, incrementalColumns, lastRecords);
        String soql = soqlBuilder.build();
        assertEquals(soql, "SELECT Id, Name FROM Account WHERE Name != 'John Doe' ORDER BY Id ASC");
    }

    @Test
    public void testBuildWithIncrementalColumnsAndLastRecords() {
        String select = "Id, Name";
        String object = "Account";
        Optional<String> where = Optional.empty();
        List<String> incrementalColumns = Arrays.asList("Id");
        Optional<List<String>> lastRecords = Optional.of(Arrays.asList("1000"));
        SoqlBuilder soqlBuilder =
                new SoqlBuilder(select, object, where, incrementalColumns, lastRecords);
        String soql = soqlBuilder.build();
        assertEquals(soql, "SELECT Id, Name FROM Account WHERE (Id > '1000') ORDER BY Id ASC");
    }

    @Test
    public void testBuildWithWhereAndIncrementalColumnsAndLastRecords() {
        String select = "Id, Name";
        String object = "Account";
        Optional<String> where = Optional.of("Name != 'John Doe'");
        List<String> incrementalColumns = Arrays.asList("Id");
        Optional<List<String>> lastRecords = Optional.of(Arrays.asList("1000"));
        SoqlBuilder soqlBuilder =
                new SoqlBuilder(select, object, where, incrementalColumns, lastRecords);
        String soql = soqlBuilder.build();
        assertEquals(
                soql,
                "SELECT Id, Name FROM Account WHERE Name != 'John Doe' AND (Id > '1000') ORDER BY Id ASC");
    }

    @Test
    public void testBuildWithWhereAndMultipleIncrementalColumnsAndLastRecords() {
        String select = "Id, Name";
        String object = "Account";
        Optional<String> where = Optional.of("Name != 'John Doe'");
        List<String> incrementalColumns = Arrays.asList("Id", "Name");
        Optional<List<String>> lastRecords = Optional.of(Arrays.asList("1000", "M"));
        SoqlBuilder soqlBuilder =
                new SoqlBuilder(select, object, where, incrementalColumns, lastRecords);
        String soql = soqlBuilder.build();
        assertEquals(
                soql,
                "SELECT Id, Name FROM Account WHERE Name != 'John Doe' AND (Id > '1000' AND Name > 'M') ORDER BY Id ASC, Name ASC");
    }
}
