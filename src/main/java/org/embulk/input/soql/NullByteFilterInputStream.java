package org.embulk.input.soql;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * An InputStream wrapper that filters out null bytes (0x00) from the underlying stream.
 *
 * <p>Salesforce Bulk API may return CSV data containing null bytes in text fields, which causes
 * Embulk's CSV parser to misinterpret them as line terminators, resulting in parse errors.
 */
class NullByteFilterInputStream extends FilterInputStream {

    private boolean nullByteDetected = false;

    NullByteFilterInputStream(InputStream in) {
        super(in);
    }

    /** Returns true if any null bytes were encountered during reading. */
    boolean hasDetectedNullBytes() {
        return nullByteDetected;
    }

    /** Reads a single byte, skipping over any null bytes (0x00). */
    @Override
    public int read() throws IOException {
        int b;
        do {
            b = super.read();
            if (b == 0) {
                nullByteDetected = true;
            }
        } while (b == 0);
        return b;
    }

    /**
     * Reads into a buffer, filtering out null bytes (0x00) in-place.
     *
     * <p>If a chunk consists entirely of null bytes, we must loop and read again rather than
     * returning 0, since returning 0 from {@code read(byte[], int, int)} signals "no data available
     * yet" per the InputStream contract, which can cause busy-loops in callers.
     */
    @Override
    public int read(byte[] buf, int off, int len) throws IOException {
        while (true) {
            int bytesRead = super.read(buf, off, len);
            if (bytesRead <= 0) {
                return bytesRead;
            }

            // Compact non-null bytes toward the front of the buffer
            int writePos = off;
            for (int i = off; i < off + bytesRead; i++) {
                if (buf[i] != 0) {
                    buf[writePos++] = buf[i];
                } else {
                    nullByteDetected = true;
                }
            }

            int filtered = writePos - off;
            if (filtered > 0) {
                return filtered;
            }
            // All bytes were null; loop to read the next chunk
        }
    }

    /** Disable mark/reset since byte positions shift after filtering. */
    @Override
    public boolean markSupported() {
        return false;
    }
}
