package org.embulk.input.soql;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import org.junit.Test;

public class TestSoqlBuilder {
    @Test
    public void testBuildWithEmptyIncrementalColumns() {
        SoqlBuilder soqlBuilder =
                new SoqlBuilder(
                        "Id, Name",
                        "Account",
                        Optional.empty(),
                        Optional.empty(),
                        Collections.emptyList(),
                        Optional.empty());
        assertEquals("SELECT Id, Name FROM Account", soqlBuilder.build());
    }

    @Test
    public void testBuildWithEmptyIncrementalColumnsAndLimit() {
        SoqlBuilder soqlBuilder =
                new SoqlBuilder(
                        "Id, Name",
                        "Account",
                        Optional.empty(),
                        Optional.of(999),
                        Collections.emptyList(),
                        Optional.empty());
        assertEquals("SELECT Id, Name FROM Account LIMIT 999", soqlBuilder.build());
    }

    @Test
    public void testBuildWithWhereAndEmptyIncrementalColumns() {
        SoqlBuilder soqlBuilder =
                new SoqlBuilder(
                        "Id, Name",
                        "Account",
                        Optional.of("Name != 'John Doe'"),
                        Optional.empty(),
                        Collections.emptyList(),
                        Optional.empty());
        assertEquals(
                "SELECT Id, Name FROM Account WHERE (Name != 'John Doe')", soqlBuilder.build());
    }

    @Test
    public void testBuildWithIncrementalColumns() {
        SoqlBuilder soqlBuilder =
                new SoqlBuilder(
                        "Id, Name",
                        "Account",
                        Optional.empty(),
                        Optional.empty(),
                        Arrays.asList("Id"),
                        Optional.empty());
        assertEquals("SELECT Id, Name FROM Account ORDER BY Id ASC", soqlBuilder.build());
    }

    @Test
    public void testBuildWithWhereAndIncrementalColumns() {
        SoqlBuilder soqlBuilder =
                new SoqlBuilder(
                        "Id, Name",
                        "Account",
                        Optional.of("Name != 'John Doe'"),
                        Optional.empty(),
                        Arrays.asList("Id"),
                        Optional.empty());
        assertEquals(
                "SELECT Id, Name FROM Account WHERE (Name != 'John Doe') ORDER BY Id ASC",
                soqlBuilder.build());
    }

    @Test
    public void testBuildWithIncrementalColumnsAndLastRecords() {
        SoqlBuilder soqlBuilder =
                new SoqlBuilder(
                        "Id, Name",
                        "Account",
                        Optional.empty(),
                        Optional.empty(),
                        Arrays.asList("Id"),
                        Optional.of(Arrays.asList("0012v00002TtF31AAF")));
        assertEquals(
                "SELECT Id, Name FROM Account WHERE (Id > '0012v00002TtF31AAF') ORDER BY Id ASC",
                soqlBuilder.build());
    }

    @Test
    public void testBuildWithWhereAndIncrementalColumnsAndLastRecords() {
        SoqlBuilder soqlBuilder =
                new SoqlBuilder(
                        "Id, Name",
                        "Account",
                        Optional.of("Name != 'John Doe'"),
                        Optional.empty(),
                        Arrays.asList("Id"),
                        Optional.of(Arrays.asList("0012v00002TtF31AAF")));
        assertEquals(
                "SELECT Id, Name FROM Account WHERE (Name != 'John Doe') AND (Id > '0012v00002TtF31AAF') ORDER BY Id ASC",
                soqlBuilder.build());
    }

    @Test
    public void testBuildWithWhereAndMultipleIncrementalColumnsAndLastRecords() {
        SoqlBuilder soqlBuilder =
                new SoqlBuilder(
                        "Id, Name",
                        "Account",
                        Optional.of("Name != 'John Doe'"),
                        Optional.empty(),
                        Arrays.asList("Id", "Name"),
                        Optional.of(Arrays.asList("0012v00002TtF31AAF", "M")));
        assertEquals(
                "SELECT Id, Name FROM Account WHERE (Name != 'John Doe') AND ((Id > '0012v00002TtF31AAF') OR (Id = '0012v00002TtF31AAF' AND Name > 'M')) ORDER BY Id ASC, Name ASC",
                soqlBuilder.build());
    }

    @Test
    public void testBuildWithFullArguments() {
        SoqlBuilder soqlBuilder =
                new SoqlBuilder(
                        "Id, Name",
                        "Account",
                        Optional.of("Name != 'John Doe'"),
                        Optional.of(999),
                        Arrays.asList("Id", "Name"),
                        Optional.of(Arrays.asList("0012v00002TtF31AAF", "M")));
        assertEquals(
                "SELECT Id, Name FROM Account WHERE (Name != 'John Doe') AND ((Id > '0012v00002TtF31AAF') OR (Id = '0012v00002TtF31AAF' AND Name > 'M')) ORDER BY Id ASC, Name ASC LIMIT 999",
                soqlBuilder.build());
    }

    @Test
    public void testBuildWithTimestampInLastRecords() {
        SoqlBuilder soqlBuilder =
                new SoqlBuilder(
                        "Id, LastModifiedDate",
                        "Account",
                        Optional.empty(),
                        Optional.empty(),
                        Arrays.asList("LastModifiedDate"),
                        Optional.of(Arrays.asList("2024-01-02T03:04:05.000Z")));
        assertEquals(
                "SELECT Id, LastModifiedDate FROM Account WHERE (LastModifiedDate > 2024-01-02T03:04:05.000Z) ORDER BY LastModifiedDate ASC",
                soqlBuilder.build());
    }

    @Test
    public void testBuildWithDateInLastRecords() {
        SoqlBuilder soqlBuilder =
                new SoqlBuilder(
                        "Id, LastModifiedDate",
                        "Account",
                        Optional.empty(),
                        Optional.empty(),
                        Arrays.asList("LastModifiedDate"),
                        Optional.of(Arrays.asList("2024-01-02")));
        assertEquals(
                "SELECT Id, LastModifiedDate FROM Account WHERE (LastModifiedDate > 2024-01-02) ORDER BY LastModifiedDate ASC",
                soqlBuilder.build());
    }

    @Test
    public void testBuildWithNumericLastRecords() {
        SoqlBuilder soqlBuilder =
                new SoqlBuilder(
                        "Id, Amount",
                        "Account",
                        Optional.empty(),
                        Optional.empty(),
                        Arrays.asList("Amount"),
                        Optional.of(Arrays.asList("42")));
        assertEquals(
                "SELECT Id, Amount FROM Account WHERE (Amount > 42) ORDER BY Amount ASC",
                soqlBuilder.build());
    }

    @Test
    public void testBuildWithSingleQuoteInLastRecords() {
        SoqlBuilder soqlBuilder =
                new SoqlBuilder(
                        "Id, Name",
                        "Account",
                        Optional.empty(),
                        Optional.empty(),
                        Arrays.asList("Name"),
                        Optional.of(Arrays.asList("O'Brien")));
        assertEquals(
                "SELECT Id, Name FROM Account WHERE (Name > 'O\\'Brien') ORDER BY Name ASC",
                soqlBuilder.build());
    }

    @Test
    public void testBuildWithBackslashInLastRecords() {
        SoqlBuilder soqlBuilder =
                new SoqlBuilder(
                        "Id, Name",
                        "Account",
                        Optional.empty(),
                        Optional.empty(),
                        Arrays.asList("Name"),
                        Optional.of(Arrays.asList("test\\value")));
        assertEquals(
                "SELECT Id, Name FROM Account WHERE (Name > 'test\\\\value') ORDER BY Name ASC",
                soqlBuilder.build());
    }

    @Test
    public void testBuildWithBackslashQuoteInLastRecords() {
        SoqlBuilder soqlBuilder =
                new SoqlBuilder(
                        "Id, Name",
                        "Account",
                        Optional.empty(),
                        Optional.empty(),
                        Arrays.asList("Name"),
                        Optional.of(Arrays.asList("test\\'end")));
        assertEquals(
                "SELECT Id, Name FROM Account WHERE (Name > 'test\\\\\\'end') ORDER BY Name ASC",
                soqlBuilder.build());
    }
}
