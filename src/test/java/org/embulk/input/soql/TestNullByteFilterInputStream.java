package org.embulk.input.soql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.Test;

public class TestNullByteFilterInputStream {

    @Test
    public void testReadSingleByte() throws IOException {
        byte[] input = {0x41, 0x00, 0x42, 0x00, 0x43}; // A \0 B \0 C
        try (NullByteFilterInputStream stream =
                new NullByteFilterInputStream(new ByteArrayInputStream(input))) {
            assertEquals('A', stream.read());
            assertEquals('B', stream.read());
            assertEquals('C', stream.read());
            assertEquals(-1, stream.read());
        }
    }

    @Test
    public void testReadSingleByteAllNulls() throws IOException {
        byte[] input = {0x00, 0x00, 0x00};
        try (NullByteFilterInputStream stream =
                new NullByteFilterInputStream(new ByteArrayInputStream(input))) {
            assertEquals(-1, stream.read());
        }
    }

    @Test
    public void testReadBuffer() throws IOException {
        byte[] input = {0x41, 0x00, 0x42, 0x00, 0x43}; // A \0 B \0 C
        try (NullByteFilterInputStream stream =
                new NullByteFilterInputStream(new ByteArrayInputStream(input))) {
            byte[] buf = new byte[10];
            int read = stream.read(buf, 0, buf.length);
            assertEquals(3, read);
            assertEquals('A', buf[0]);
            assertEquals('B', buf[1]);
            assertEquals('C', buf[2]);
        }
    }

    @Test
    public void testReadBufferAllNulls() throws IOException {
        byte[] input = {0x00, 0x00, 0x00};
        try (NullByteFilterInputStream stream =
                new NullByteFilterInputStream(new ByteArrayInputStream(input))) {
            byte[] buf = new byte[10];
            int read = stream.read(buf, 0, buf.length);
            assertEquals(-1, read);
        }
    }

    @Test
    public void testReadBufferNoNulls() throws IOException {
        byte[] input = {0x41, 0x42, 0x43}; // A B C
        try (NullByteFilterInputStream stream =
                new NullByteFilterInputStream(new ByteArrayInputStream(input))) {
            byte[] buf = new byte[10];
            int read = stream.read(buf, 0, buf.length);
            assertEquals(3, read);
            assertEquals('A', buf[0]);
            assertEquals('B', buf[1]);
            assertEquals('C', buf[2]);
        }
    }

    @Test
    public void testReadBufferWithOffset() throws IOException {
        byte[] input = {0x41, 0x00, 0x42};
        try (NullByteFilterInputStream stream =
                new NullByteFilterInputStream(new ByteArrayInputStream(input))) {
            byte[] buf = new byte[10];
            buf[0] = 0x58; // X - should not be overwritten
            int read = stream.read(buf, 1, 9);
            assertEquals(2, read);
            assertEquals('X', buf[0]); // untouched
            assertEquals('A', buf[1]);
            assertEquals('B', buf[2]);
        }
    }

    @Test
    public void testEmptyStream() throws IOException {
        byte[] input = {};
        try (NullByteFilterInputStream stream =
                new NullByteFilterInputStream(new ByteArrayInputStream(input))) {
            assertEquals(-1, stream.read());

            byte[] buf = new byte[10];
            assertEquals(-1, stream.read(buf, 0, buf.length));
        }
    }

    @Test
    public void testCsvWithNullBytes() throws IOException {
        // Simulate Salesforce Bulk API CSV with null byte in a field value
        String csv = "\"Id\",\"Name\",\"Field__c\"\n\"001\",\"test\",\"before\u0000after\"\n";
        byte[] input = csv.getBytes("UTF-8");
        try (NullByteFilterInputStream stream =
                new NullByteFilterInputStream(new ByteArrayInputStream(input))) {
            byte[] buf = new byte[1024];
            int read = stream.read(buf, 0, buf.length);

            String result = new String(buf, 0, read, "UTF-8");
            String expected = "\"Id\",\"Name\",\"Field__c\"\n\"001\",\"test\",\"beforeafter\"\n";
            assertEquals(expected, result);
        }
    }

    @Test
    public void testNormalCsvWithoutNullBytes() throws IOException {
        // Typical Salesforce Bulk API CSV with no null bytes - should pass through unchanged
        String csv =
                "\"Id\",\"Name\",\"Description\"\n"
                        + "\"001ABC\",\"Acme Corp\",\"Some description\"\n"
                        + "\"002DEF\",\"Test Inc\",\"Another row\"\n";
        byte[] input = csv.getBytes("UTF-8");
        try (NullByteFilterInputStream stream =
                new NullByteFilterInputStream(new ByteArrayInputStream(input))) {
            byte[] buf = new byte[1024];
            int read = stream.read(buf, 0, buf.length);
            String result = new String(buf, 0, read, "UTF-8");
            assertEquals(csv, result);
        }
    }

    @Test
    public void testNormalCsvWithMultibyteCharacters() throws IOException {
        String csv = "\"Id\",\"Name\"\n\"001\",\"日本語テスト\"\n";
        byte[] input = csv.getBytes("UTF-8");
        try (NullByteFilterInputStream stream =
                new NullByteFilterInputStream(new ByteArrayInputStream(input))) {
            byte[] buf = new byte[1024];
            int read = stream.read(buf, 0, buf.length);
            String result = new String(buf, 0, read, "UTF-8");
            assertEquals(csv, result);
        }
    }

    @Test
    public void testNormalCsvWithNewlinesInQuotedField() throws IOException {
        // Quoted field containing newlines - common in Salesforce long text areas
        String csv = "\"Id\",\"Notes\"\n\"001\",\"line1\nline2\nline3\"\n";
        byte[] input = csv.getBytes("UTF-8");
        try (NullByteFilterInputStream stream =
                new NullByteFilterInputStream(new ByteArrayInputStream(input))) {
            byte[] buf = new byte[1024];
            int read = stream.read(buf, 0, buf.length);
            String result = new String(buf, 0, read, "UTF-8");
            assertEquals(csv, result);
        }
    }

    @Test
    public void testMarkNotSupported() throws IOException {
        byte[] input = {0x41};
        try (NullByteFilterInputStream stream =
                new NullByteFilterInputStream(new ByteArrayInputStream(input))) {
            assertFalse(stream.markSupported());
        }
    }

    @Test
    public void testNullBytesAtBoundaries() throws IOException {
        // Null bytes at start and end
        byte[] input = {0x00, 0x41, 0x42, 0x00};
        try (NullByteFilterInputStream stream =
                new NullByteFilterInputStream(new ByteArrayInputStream(input))) {
            byte[] buf = new byte[10];
            int read = stream.read(buf, 0, buf.length);
            assertEquals(2, read);
            assertEquals('A', buf[0]);
            assertEquals('B', buf[1]);
        }
    }

    @Test
    public void testHasDetectedNullBytesWhenPresent() throws IOException {
        byte[] input = {0x41, 0x00, 0x42};
        try (NullByteFilterInputStream stream =
                new NullByteFilterInputStream(new ByteArrayInputStream(input))) {
            byte[] buf = new byte[10];
            stream.read(buf, 0, buf.length);
            assertTrue(stream.hasDetectedNullBytes());
        }
    }

    @Test
    public void testHasDetectedNullBytesWhenAbsent() throws IOException {
        byte[] input = {0x41, 0x42, 0x43};
        try (NullByteFilterInputStream stream =
                new NullByteFilterInputStream(new ByteArrayInputStream(input))) {
            byte[] buf = new byte[10];
            stream.read(buf, 0, buf.length);
            assertFalse(stream.hasDetectedNullBytes());
        }
    }

    @Test
    public void testHasDetectedNullBytesWithSingleByteRead() throws IOException {
        byte[] input = {0x41, 0x00, 0x42};
        try (NullByteFilterInputStream stream =
                new NullByteFilterInputStream(new ByteArrayInputStream(input))) {
            stream.read();
            stream.read();
            assertTrue(stream.hasDetectedNullBytes());
        }
    }
}
