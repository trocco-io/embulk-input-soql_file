package org.embulk.input.soql;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import org.embulk.spi.util.InputStreamFileInput;
import org.embulk.spi.util.InputStreamFileInput.InputStreamWithHints;

public class SingleFileProvider implements InputStreamFileInput.Provider
{
    private Iterator<InputStream> iterator;

    public SingleFileProvider(List<InputStream> inputStreams)
    {
        this.iterator = inputStreams.iterator();
    }

    @Override
    public InputStreamWithHints openNextWithHints()
    {
        if (!iterator.hasNext()) {
            return null;
        }
        return new InputStreamWithHints(iterator.next());
    }

    @Override
    public void close() throws IOException
    {
    }

}
