package com.threshold.toolbox;

import android.os.*;
import android.util.Log;

import java.io.File;
import java.io.OutputStream;

public class AsyncFileWriter extends OutputStream {

    private static final String TAG = "AsyncFileWriter";

    private final RingBuffer mRingBuf;
    private final FileWriterWorker mFileWriterWorker;

    public AsyncFileWriter(final File file, final int bufferSize) {
        if (null == file || bufferSize < 4096) {
            throw new IllegalArgumentException(String.format("file is null or bufferSize=%d " +
                    "too small(at least 4096)", bufferSize));
        }
        mRingBuf = new RingBuffer(bufferSize);
        mFileWriterWorker = new FileWriterWorker(file, bufferSize / 8, mRingBuf);
    }

    private void checkCloseStatus() {
        if (mFileWriterWorker.isClosed()) {
            throw new IllegalStateException("AsyncFileWriter is already closed");
        }
    }

    @Override
    public void write(final int b) {
        checkCloseStatus();
        throw new UnsupportedOperationException("unsupported write(int)," +
                " consider convert int to bytes instead");
    }

    @Override
    public void write(final byte[] data, final int offset, final int len) {
        checkCloseStatus();
        if (mRingBuf.availableWriteLen() < len) {
            Log.w(TAG, "Performance Warning: i/o can't catch your output data speed");
            final byte[] dataCpy = new byte[len];
            System.arraycopy(data, offset, dataCpy, 0, len);
            mFileWriterWorker.sendWriteDataMsg(dataCpy, len);
            return;
        }
        int writeLen;
        if (len != (writeLen = mRingBuf.write(data, offset, len))) {
            throw new RuntimeException(String.format("!bug, abnormal write, " +
                    "expect=%d, real_write=%d", len, writeLen));
        }
        mFileWriterWorker.sendWriteDataMsg(len);
    }

    @Override
    public void write(final byte[] data) {
        write(data, 0, data.length);
    }

    @Override
    public void flush() {
        checkCloseStatus();
        mFileWriterWorker.sendFlushMsg();
    }

    @Override
    public void close() {
        checkCloseStatus();
        mFileWriterWorker.sendCloseMsg();
    }

    private static class FileWriterWorker implements Handler.Callback {
        private static final int MSG_WHAT_WRITE = 1;
        private static final int MSG_WHAT_FLUSH = 2;
        private static final int MSG_WHAT_CLOSE = 3;

        private FileWriter mFileWriter;
        private boolean mIsClosed = false;
        private byte[] mReadBuf = new byte[8192];
        private final RingBuffer mRingBuf;
        private final Handler mHandler;

        private FileWriterWorker(File file, int bufferSize, RingBuffer ringBuf) {
            final HandlerThread fileWriterHandlerThread = new HandlerThread("AsyncFWThr");
            fileWriterHandlerThread.start();
            mFileWriter = new FileWriter(file, bufferSize);
            mRingBuf = ringBuf;
            mHandler = new Handler(fileWriterHandlerThread.getLooper(), this);
        }

        void sendWriteDataMsg(byte[] data, int len) {
            if (mIsClosed) {
                return;
            }
            mHandler.obtainMessage(MSG_WHAT_WRITE, len, 0, data).sendToTarget();
        }

        void sendWriteDataMsg(int len) {
            sendWriteDataMsg(null, len);
        }

        void sendFlushMsg() {
            if (mIsClosed) {
                return;
            }
            mHandler.obtainMessage(MSG_WHAT_FLUSH).sendToTarget();
        }

        void sendCloseMsg() {
            if (mIsClosed) {
                return;
            }
            mIsClosed = true;
            mHandler.obtainMessage(MSG_WHAT_CLOSE).sendToTarget();
        }

        boolean isClosed() {
            return mIsClosed;
        }

        @Override
        public boolean handleMessage(final Message msg) {
            switch (msg.what) {
                case MSG_WHAT_WRITE: {
                    final byte[] data = (byte[]) msg.obj;
                    final int len = msg.arg1;
                    if (data != null) { // <== new data coming, not from ring buffer.
                        mFileWriter.write(data, 0, len);
                        break;
                    }
                    if (mReadBuf.length < len) {
                        mReadBuf = new byte[len * 2]; // expand read buffer
                    }
                    int readLen;
                    if (len == (readLen = mRingBuf.read(mReadBuf, 0, len))) {
                        mFileWriter.write(mReadBuf, 0, len);
                    } else {
                        throw new RuntimeException(String.format("!!!bug, abnormal read from " +
                                "ring(expect=%d, read=%d)", len, readLen));
                    }
                }
                break;
                case MSG_WHAT_FLUSH:
                    mFileWriter.flush();
                    break;
                case MSG_WHAT_CLOSE:
                    mFileWriter.close();
                    mFileWriter = null;
                    mRingBuf.close();
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                            mHandler.getLooper().quitSafely();
                        } else {
                            mHandler.getLooper().quit();
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    break;
                default:
                    Log.e(TAG, "unknown msg.what=" + msg.what);
                    break;
            }
            return true;
        }

    }


}
