package com.threshold.toolbox;

import androidx.annotation.NonNull;
import android.os.*;
import android.util.Log;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

public class AsyncFileWriter extends OutputStream {

    private static final String TAG = "AsyncFileWriter";
    private static final int MIN_BUFFER_SIZE = 4096;
    private static final int DEFAULT_CHUNK_SIZE = 8192;

    public interface ErrorCallback {
        void onError(String operation, Exception ex);
    }

    private final RingBuffer mRingBuf;
    private final FileWriterWorker mFileWriterWorker;
    private final byte[] mSingleByteBuf = new byte[1];
    private volatile boolean mClosed;
    private ErrorCallback mErrorCallback;

    public AsyncFileWriter(final String path, final int bufferSize) {
        this(new File(path), bufferSize, null);
    }

    public AsyncFileWriter(final File file, final int bufferSize) {
        this(file, bufferSize, null);
    }

    public AsyncFileWriter(final File file, final int bufferSize, final ErrorCallback callback) {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }
        if (bufferSize < MIN_BUFFER_SIZE) {
            throw new IllegalArgumentException("Buffer size too small (min " +
                    MIN_BUFFER_SIZE + " bytes)");
        }
        mErrorCallback = callback;
        mRingBuf = new RingBuffer(bufferSize);
        mFileWriterWorker = new FileWriterWorker(file, DEFAULT_CHUNK_SIZE, mRingBuf);
    }

    public void setErrorCallback(ErrorCallback callback) {
        mErrorCallback = callback;
    }

    private void checkCloseStatus() {
        if (mClosed) {
            throw new IllegalStateException("AsyncFileWriter is closed");
        }
    }

    @Override
    public void write(final int b) throws IOException {
        checkCloseStatus();
        mSingleByteBuf[0] = (byte) b;
        write(mSingleByteBuf, 0, 1);
    }

    @Override
    public void write(final byte[] data, final int offset, final int len) throws IOException {
        checkCloseStatus();
        if (data == null) {
            throw new NullPointerException("Data cannot be null");
        }
        if (offset < 0 || len < 0 || offset + len > data.length) {
            throw new IndexOutOfBoundsException("Invalid offset/length");
        }

        int remaining = len;
        int currentOffset = offset;

        while (remaining > 0) {
            final int writable = Math.min(mRingBuf.availableWriteLen(), remaining);
            if (writable > 0) {
                int written = mRingBuf.write(data, currentOffset, writable);
                if (written != writable) {
                    handleWorkerError("RingBuffer write mismatch",
                            new IOException("RingBuffer write error"));
                }
                mFileWriterWorker.scheduleWrite();
                currentOffset += written;
                remaining -= written;
            } else {
                // 缓冲区满时等待并重试
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    handleWorkerError("Write interrupted", e);
                }
            }
        }
    }

    @Override
    public void write(final byte[] data) throws IOException {
        write(data, 0, data.length);
    }

    @Override
    public void flush() {
        checkCloseStatus();
        mFileWriterWorker.sendFlushMsg();
    }

    @Override
    public void close() {
        if (mClosed) {
            return;
        }
        mClosed = true;
        mFileWriterWorker.sendCloseMsg();
    }

    private void handleWorkerError(String message, Exception ex) {
        Log.e(TAG, message, ex);
        if (mErrorCallback != null) {
            mErrorCallback.onError(message, ex);
        }
        close(); // 发生错误时自动关闭
    }

    private class FileWriterWorker implements Handler.Callback {
        private static final int MSG_WHAT_WRITE = 1;
        private static final int MSG_WHAT_FLUSH = 2;
        private static final int MSG_WHAT_CLOSE = 3;

        private FileWriter mFileWriter;
        private volatile boolean mWorkerClosed;
        private final byte[] mChunkBuffer;
        private final RingBuffer mRingBuf;
        private final Handler mHandler;
        private final HandlerThread mHandlerThread;

        FileWriterWorker(File file, int chunkSize, RingBuffer ringBuf) {
            mHandlerThread = new HandlerThread("AsyncFileWriter");
            mHandlerThread.start();
            try {
                mFileWriter = new FileWriter(file, chunkSize);
            } catch (FileNotFoundException e) {
                handleWorkerError("File creation failed", e);
            }
            mRingBuf = ringBuf;
            mChunkBuffer = new byte[chunkSize];
            mHandler = new Handler(mHandlerThread.getLooper(), this);
        }

        void scheduleWrite() {
            if (mWorkerClosed) return;
            mHandler.removeMessages(MSG_WHAT_WRITE);
            mHandler.sendEmptyMessage(MSG_WHAT_WRITE);
        }

        void sendFlushMsg() {
            if (mWorkerClosed) return;
            mHandler.sendEmptyMessage(MSG_WHAT_FLUSH);
        }

        void sendCloseMsg() {
            if (mWorkerClosed) return;
            mWorkerClosed = true;
            mHandler.sendEmptyMessage(MSG_WHAT_CLOSE);
        }

        @Override
        public boolean handleMessage(@NonNull final Message msg) {
            if (mFileWriter == null) return true;

            try {
                switch (msg.what) {
                    case MSG_WHAT_WRITE:
                        handleWrite();
                        break;
                    case MSG_WHAT_FLUSH:
                        mFileWriter.flush();
                        break;
                    case MSG_WHAT_CLOSE:
                        handleClose();
                        break;
                    default:
                        Log.w(TAG, "Unknown message: " + msg.what);
                }
            } catch (Exception e) {
                handleWorkerError("Worker operation failed", e);
            }
            return true;
        }

        private void handleWrite() throws IOException {
            int available = mRingBuf.availableReadLen();
            if (available <= 0) return;

            int toRead = Math.min(available, mChunkBuffer.length);
            int read = mRingBuf.read(mChunkBuffer, 0, toRead);
            if (read > 0) {
                mFileWriter.write(mChunkBuffer, 0, read);
            }

            // 如果还有数据，再次调度写入
            if (mRingBuf.availableReadLen() > 0) {
                scheduleWrite();
            }
        }

        private void handleClose() {
            try {
                // 写入剩余数据
                while (mRingBuf.availableReadLen() > 0) {
                    handleWrite();
                }

                mFileWriter.flush();
            } catch (Exception e) {
                handleWorkerError("Final flush failed", e);
            } finally {
                try {
                    mFileWriter.close();
                } catch (Exception e) {
                    Log.w(TAG, "Close error: " + e.getMessage());
                }
                mRingBuf.close();

                // 确保线程退出
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    mHandlerThread.quitSafely();
                } else {
                    mHandlerThread.quit();
                }
            }
        }
    }
}