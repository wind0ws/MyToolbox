package com.threshold.toolbox;

import android.util.Log;
import java.io.*;

public class FileWriter extends OutputStream {

    private static final String TAG = "FileWriter";
    private static final int DEFAULT_BUFFER_SIZE = 8192; // 8KB

    private OutputStream mOutputStream;
    private boolean mHasError;
    private boolean mClosed;

    public FileWriter(final File file, final int bufferSize) throws FileNotFoundException {
        if (file == null) {
            throw new NullPointerException("File cannot be null");
        }
        final int actualBufferSize = bufferSize > 0 ? bufferSize : DEFAULT_BUFFER_SIZE;
        try {
            mOutputStream = new BufferedOutputStream(
                    new FileOutputStream(file), actualBufferSize);
        } catch (FileNotFoundException ex) {
            mHasError = true;
            Log.e(TAG, "File not found: " + file.getAbsolutePath(), ex);
            throw ex;
        }
    }

    public FileWriter(final String path) throws FileNotFoundException {
        this(new File(path), 0);
    }

    private void ensureValidState() {
        if (mClosed) {
            throw new IllegalStateException("Stream already closed");
        }
        if (mHasError) {
            throw new IllegalStateException("Stream in error state");
        }
    }

    @Override
    public void write(final int b) throws IOException {
        ensureValidState();
        try {
            mOutputStream.write(b);
        } catch (IOException e) {
            handleException("write(int)", e);
        }
    }

    @Override
    public void write(final byte[] buffer, final int offset, int len) throws IOException {
        ensureValidState();
        try {
            mOutputStream.write(buffer, offset, len);
        } catch (IOException ex) {
            handleException("write(byte[])", ex);
        }
    }

    @Override
    public void write(final byte[] buffer) throws IOException {
        write(buffer, 0, buffer.length);
    }

    @Override
    public void flush() throws IOException {
        if (mClosed || mHasError || mOutputStream == null) {
            return;
        }
        try {
            mOutputStream.flush();
        } catch (IOException ex) {
            handleException("flush", ex);
        }
    }

    private void handleException(String operation, IOException ex) throws IOException {
        mHasError = true;
        Log.e(TAG, "Error during " + operation + ": " + ex.getMessage(), ex);
        safeClose();
        throw ex;
    }

    private void safeClose() {
        try {
            if (mOutputStream != null) {
                mOutputStream.close();
            }
        } catch (IOException e) {
            Log.w(TAG, "Error closing stream: " + e.getMessage());
        } finally {
            mOutputStream = null;
        }
    }

    @Override
    public void close() {
        if (mClosed) {
            return;
        }
        mClosed = true;
        try {
            flush();
        } catch (Exception e) {
            Log.w(TAG, "Error flushing before close", e);
        }
        safeClose();
    }
}