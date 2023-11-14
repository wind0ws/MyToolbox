package com.threshold.toolbox;

import android.util.Log;
import java.io.*;

public class FileWriter extends OutputStream {

    private static final String TAG = "FileWriter";
    private OutputStream mOutputStream;

    public FileWriter(final File file, final int bufferSize) {
        try {
            if (bufferSize < 4096) {
                mOutputStream = new FileOutputStream(file);
            } else {
                mOutputStream = new BufferedOutputStream(new FileOutputStream(file), bufferSize);
            }
        } catch (FileNotFoundException ex) {
            Log.e(TAG, "error on create OutputStream", ex);
            throw new RuntimeException(file + " not found", ex);
        }
    }

    public FileWriter(final String path) {
        this(new File(path), 0);
    }

    @Override
    public void write(final int b) {
        try {
            mOutputStream.write(b);
        } catch (IOException e) {
            Log.e(TAG, "error on write", e);
        }
    }

    @Override
    public void write(final byte[] buffer, final int offset, int len) {
        try {
            mOutputStream.write(buffer, offset, len);
        } catch (IOException ex) {
            Log.e(TAG, "error on write", ex);
        }
    }

    @Override
    public void write(final byte[] buffer) {
        write(buffer, 0, buffer.length);
    }

    @Override
    public void flush() {
        if (mOutputStream == null) {
            return;
        }
        try {
            mOutputStream.flush();
        } catch (IOException ex) {
            Log.e(TAG, "error on flush", ex);
        }
    }

    @Override
    public void close() {
        if (mOutputStream == null) {
            return;
        }
        try {
            mOutputStream.close();
        } catch (IOException ex) {
            Log.e(TAG, "error on close", ex);
        }
        mOutputStream = null;
    }

}
