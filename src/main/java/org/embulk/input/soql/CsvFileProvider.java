package org.embulk.input.soql;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import org.embulk.util.file.InputStreamFileInput;
import org.embulk.util.file.InputStreamFileInput.InputStreamWithHints;

public class CsvFileProvider implements InputStreamFileInput.Provider {
    private List<Path> csvFilePaths;
    private Iterator<Path> iterator;

    public CsvFileProvider(List<Path> csvFilePaths) {
        this.csvFilePaths = csvFilePaths;
        this.iterator = csvFilePaths.iterator();
    }

    @Override
    public InputStreamWithHints openNextWithHints() throws IOException {
        if (!iterator.hasNext()) {
            return null;
        }
        return new InputStreamWithHints(Files.newInputStream(iterator.next()));
    }

    @Override
    public void close() throws IOException {}
}
