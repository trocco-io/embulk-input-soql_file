package org.embulk.input.soql;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.Test;

public class TestSoqlBuilder {
    @Test
    public void testBuildWithEmptyIncrementalColumns() {
        String select = "Id, Name";
        String object = "Account";
        Optional<String> where = Optional.empty();
        Optional<Integer> limit = Optional.empty();
        List<String> incrementalColumns = Collections.emptyList();
        Optional<List<String>> lastRecords = Optional.empty();
        SoqlBuilder soqlBuilder =
                new SoqlBuilder(select, object, where, limit, incrementalColumns, lastRecords);
        String soql = soqlBuilder.build();
        assertEquals(soql, "SELECT Id, Name FROM Account");
    }

    @Test
    public void testBuildWithEmptyIncrementalColumnsAndLimit() {
        String select = "Id, Name";
        String object = "Account";
        Optional<String> where = Optional.empty();
        Optional<Integer> limit = Optional.of(999);
        List<String> incrementalColumns = Collections.emptyList();
        Optional<List<String>> lastRecords = Optional.empty();
        SoqlBuilder soqlBuilder =
                new SoqlBuilder(select, object, where, limit, incrementalColumns, lastRecords);
        String soql = soqlBuilder.build();
        assertEquals(soql, "SELECT Id, Name FROM Account LIMIT 999");
    }

    @Test
    public void testBuildWithWhereAndEmptyIncrementalColumns() {
        String select = "Id, Name";
        String object = "Account";
        Optional<String> where = Optional.of("Name != 'John Doe'");
        Optional<Integer> limit = Optional.empty();
        List<String> incrementalColumns = Collections.emptyList();
        Optional<List<String>> lastRecords = Optional.empty();
        SoqlBuilder soqlBuilder =
                new SoqlBuilder(select, object, where, limit, incrementalColumns, lastRecords);
        String soql = soqlBuilder.build();
        assertEquals(soql, "SELECT Id, Name FROM Account WHERE Name != 'John Doe'");
    }

    @Test
    public void testBuildWithIncrementalColumns() {
        String select = "Id, Name";
        String object = "Account";
        Optional<String> where = Optional.empty();
        Optional<Integer> limit = Optional.empty();
        List<String> incrementalColumns = Arrays.asList("Id");
        Optional<List<String>> lastRecords = Optional.empty();
        SoqlBuilder soqlBuilder =
                new SoqlBuilder(select, object, where, limit, incrementalColumns, lastRecords);
        String soql = soqlBuilder.build();
        assertEquals(soql, "SELECT Id, Name FROM Account ORDER BY Id ASC");
    }

    @Test
    public void testBuildWithWhereAndIncrementalColumns() {
        String select = "Id, Name";
        String object = "Account";
        Optional<String> where = Optional.of("Name != 'John Doe'");
        Optional<Integer> limit = Optional.empty();
        List<String> incrementalColumns = Arrays.asList("Id");
        Optional<List<String>> lastRecords = Optional.empty();
        SoqlBuilder soqlBuilder =
                new SoqlBuilder(select, object, where, limit, incrementalColumns, lastRecords);
        String soql = soqlBuilder.build();
        assertEquals(soql, "SELECT Id, Name FROM Account WHERE Name != 'John Doe' ORDER BY Id ASC");
    }

    @Test
    public void testBuildWithIncrementalColumnsAndLastRecords() {
        String select = "Id, Name";
        String object = "Account";
        Optional<String> where = Optional.empty();
        Optional<Integer> limit = Optional.empty();
        List<String> incrementalColumns = Arrays.asList("Id");
        Optional<List<String>> lastRecords = Optional.of(Arrays.asList("0012v00002TtF31AAF"));
        SoqlBuilder soqlBuilder =
                new SoqlBuilder(select, object, where, limit, incrementalColumns, lastRecords);
        String soql = soqlBuilder.build();
        assertEquals(
                soql,
                "SELECT Id, Name FROM Account WHERE (Id > '0012v00002TtF31AAF') ORDER BY Id ASC");
    }

    @Test
    public void testBuildWithWhereAndIncrementalColumnsAndLastRecords() {
        String select = "Id, Name";
        String object = "Account";
        Optional<String> where = Optional.of("Name != 'John Doe'");
        Optional<Integer> limit = Optional.empty();
        List<String> incrementalColumns = Arrays.asList("Id");
        Optional<List<String>> lastRecords = Optional.of(Arrays.asList("0012v00002TtF31AAF"));
        SoqlBuilder soqlBuilder =
                new SoqlBuilder(select, object, where, limit, incrementalColumns, lastRecords);
        String soql = soqlBuilder.build();
        assertEquals(
                soql,
                "SELECT Id, Name FROM Account WHERE Name != 'John Doe' AND (Id > '0012v00002TtF31AAF') ORDER BY Id ASC");
    }

    @Test
    public void testBuildWithWhereAndMultipleIncrementalColumnsAndLastRecords() {
        String select = "Id, Name";
        String object = "Account";
        Optional<String> where = Optional.of("Name != 'John Doe'");
        Optional<Integer> limit = Optional.empty();
        List<String> incrementalColumns = Arrays.asList("Id", "Name");
        Optional<List<String>> lastRecords = Optional.of(Arrays.asList("0012v00002TtF31AAF", "M"));
        SoqlBuilder soqlBuilder =
                new SoqlBuilder(select, object, where, limit, incrementalColumns, lastRecords);
        String soql = soqlBuilder.build();
        assertEquals(
                soql,
                "SELECT Id, Name FROM Account WHERE Name != 'John Doe' AND ((Id > '0012v00002TtF31AAF') OR (Id = '0012v00002TtF31AAF' AND Name > 'M')) ORDER BY Id ASC, Name ASC");
    }

    @Test
    public void testBuildWithFullArguments() {
        String select = "Id, Name";
        String object = "Account";
        Optional<String> where = Optional.of("Name != 'John Doe'");
        Optional<Integer> limit = Optional.of(999);
        List<String> incrementalColumns = Arrays.asList("Id", "Name");
        Optional<List<String>> lastRecords = Optional.of(Arrays.asList("0012v00002TtF31AAF", "M"));
        SoqlBuilder soqlBuilder =
                new SoqlBuilder(select, object, where, limit, incrementalColumns, lastRecords);
        String soql = soqlBuilder.build();
        assertEquals(
                soql,
                "SELECT Id, Name FROM Account WHERE Name != 'John Doe' AND ((Id > '0012v00002TtF31AAF') OR (Id = '0012v00002TtF31AAF' AND Name > 'M')) ORDER BY Id ASC, Name ASC LIMIT 999");
    }

    @Test
    public void testBuildWithTimestampInLastRecords() {
        String select = "Id, LastModifiedDate";
        String object = "Account";
        Optional<String> where = Optional.empty();
        Optional<Integer> limit = Optional.empty();
        List<String> incrementalColumns = Arrays.asList("LastModifiedDate");
        Optional<List<String>> lastRecords = Optional.of(Arrays.asList("2024-01-02T03:04:05.000Z"));
        SoqlBuilder soqlBuilder =
                new SoqlBuilder(select, object, where, limit, incrementalColumns, lastRecords);
        String soql = soqlBuilder.build();
        assertEquals(
                soql,
                "SELECT Id, LastModifiedDate FROM Account WHERE (LastModifiedDate > 2024-01-02T03:04:05.000Z) ORDER BY LastModifiedDate ASC");
    }

    @Test
    public void testBuildWithDateInLastRecords() {
        String select = "Id, LastModifiedDate";
        String object = "Account";
        Optional<String> where = Optional.empty();
        Optional<Integer> limit = Optional.empty();
        List<String> incrementalColumns = Arrays.asList("LastModifiedDate");
        Optional<List<String>> lastRecords = Optional.of(Arrays.asList("2024-01-02"));
        SoqlBuilder soqlBuilder =
                new SoqlBuilder(select, object, where, limit, incrementalColumns, lastRecords);
        String soql = soqlBuilder.build();
        assertEquals(
                soql,
                "SELECT Id, LastModifiedDate FROM Account WHERE (LastModifiedDate > 2024-01-02) ORDER BY LastModifiedDate ASC");
    }
}
