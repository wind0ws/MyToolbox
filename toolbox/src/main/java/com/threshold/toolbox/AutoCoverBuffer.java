package com.threshold.toolbox;

import androidx.annotation.Keep;

import com.threshold.jni.AutoCoverBufferJni;

import java.io.Closeable;

/**
 * Multi Consumer(read) and Multi Producer(write) AutoCoverBuffer.
 */
@Keep
public class AutoCoverBuffer implements Closeable {

    // store auto cover buffer native handle. why use long: for compat with 64bit OS
    private long mNativeHandle;
    // current write byte position. maybe overflow, that is normal behaviour.
    private int mWritePos = 0;

    /**
     * init auto cover buffer with size
     * <p>the bufferSize should be pow of 2, otherwise it will be aligned to the nearest pow of 2. </p>
     *
     * @param bufferSize capacity of ring in bytes
     */
    public AutoCoverBuffer(int bufferSize) {
        if (bufferSize < 2) {
            throw new IllegalArgumentException(String.format("illegal bufferSize(%d)", bufferSize));
        }
        final long[] nativeHandleHolder = new long[]{0};
        int ret = AutoCoverBufferJni.create(nativeHandleHolder, bufferSize);
        if (0 != ret || 0 == nativeHandleHolder[0]) {
            throw new IllegalArgumentException(String.format("failed on create auto cover buffer jni. bufferSize=%d", bufferSize));
        }
        mNativeHandle = nativeHandleHolder[0];
    }

    /**
     * get available read len in ring by readPos
     *
     * @param readPos read position
     * @return available read len in bytes. if returned negative number, that means error.
     */
    public int availableReadLen(int readPos) {
        return AutoCoverBufferJni.availableRead(mNativeHandle, readPos);
    }

    /**
     * get current write position
     *
     * @return write position
     */
    public int getWritePos() {
        return mWritePos;
    }

    /**
     * write data to ring
     *
     * @param data   the data you want to write to ring
     * @param offset data offset
     * @param len    how long you want to write to ring
     * @return real write data len, negative return value means error occurred
     */
    public synchronized int write(final byte[] data, int offset, int len) {
        int ret = AutoCoverBufferJni.write(mNativeHandle, data, offset, len);
        if (ret > 0) {
            mWritePos += ret;
        }
        return ret;
    }

    /**
     * write byte data to ring
     *
     * @param data the data you want to write to ring
     * @return real write data len
     */
    public int write(final byte[] data) {
        return write(data, 0, data.length);
    }

    /**
     * read data from ring by read position.
     *
     * @param readPos read position
     * @param buffer  store read out data
     * @param offset  buffer start offset
     * @param len     how long you want to read
     * @return real read out data len
     */
    public synchronized int read(int readPos, final byte[] buffer, int offset, int len) {
        return AutoCoverBufferJni.read(mNativeHandle, readPos, buffer, offset, len);
    }

    /**
     * read data from ring
     *
     * @param readPos read position
     * @param buffer  store read out data
     * @return real read out data len
     */
    public int read(int readPos, final byte[] buffer) {
        return read(readPos, buffer, 0, buffer.length);
    }

    /**
     * close the ring and free it's memory on native.
     * <p>do not use this instance any more after call this!</p>
     */
    @Override
    public void close() {
        if (0 != mNativeHandle) {
            final long[] nativeHandleHolder = new long[]{mNativeHandle};
            AutoCoverBufferJni.destroy(nativeHandleHolder);
            mNativeHandle = 0;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        // final safety, try save you! do not relay on it, you should close it by yourself!!!
        close();
        super.finalize();
    }

}
