package com.threshold.toolbox;

import android.util.Log;

import com.threshold.jni.RingBufferJni;

import java.io.Closeable;

/**
 * One Consumer(read) and One Producer(write) Thread-Safe RingBuffer.
 *
 * <p> not all of method are thread-safe(such as clear),
 * read/write thread-safe only in this condition: 1 consumer and 1 producer </p>
 */
public class RingBuffer implements Closeable {

    // store ring buffer native handle. why use long: for compat with 64bit OS
    private final long[] mNativeHandle = new long[]{0};
    // store ring buffer can read data len
    private final int[] mReceiveAvailableReadLen = new int[]{0};
    // store ring buffer can write data len
    private final int[] mReceiveAvailableWriteLen = new int[]{0};

    /**
     * init ring buffer with size
     * <p>the bufferSize should be pow of 2, otherwise it will be aligned to the nearest pow of 2. </p>
     * @param bufferSize capacity of ring in bytes
     */
    public RingBuffer(int bufferSize) {
        if (bufferSize < 2) {
            throw new IllegalArgumentException(String.format("illegal bufferSize(%d)", bufferSize));
        }
        int ret = RingBufferJni.create(mNativeHandle, bufferSize);
        if (0 != ret || 0 == mNativeHandle[0]) {
            throw new IllegalArgumentException(
                    String.format("failed on create ring buffer jni. bufferSize=%d", bufferSize));
        }
    }

    /**
     * get used byte len in ring
     * @return ring used len in bytes
     */
    public int availableReadLen() {
        RingBufferJni.availableRead(mNativeHandle[0], mReceiveAvailableReadLen);
        return mReceiveAvailableReadLen[0];
    }

    /**
     * get free space in ring
     * @return ring free space len in bytes
     */
    public int availableWriteLen() {
        RingBufferJni.availableWrite(mNativeHandle[0], mReceiveAvailableWriteLen);
        return mReceiveAvailableWriteLen[0];
    }

    /**
     * detect ring whether has data to read
     * @return true for empty
     */
    public boolean isEmpty() {
       return availableReadLen() < 1;
    }

    /**
     * detect ring whether has space to write
     * @return true for full
     */
    public boolean isFull() {
        return availableWriteLen() < 1;
    }

    /**
     * write data to ring
     * @param data the data you want to write to ring
     * @param offset data offset
     * @param len how long you want to write to ring
     * @return real write data len
     */
    public int write(final byte[] data, int offset, int len) {
        return RingBufferJni.write(mNativeHandle[0], data, offset, len);
    }

    public int write(final byte[] data) {
        return write(data, 0, data.length);
    }

    /**
     * read data from ring
     * @param buffer store read out data
     * @param offset buffer start offset
     * @param len how long you want to read
     * @return real read out data len
     */
    public int read(final byte[] buffer, int offset, int len) {
        return RingBufferJni.read(mNativeHandle[0], buffer, offset, len);
    }

    public int read(final byte[] buffer) {
        return read(buffer, 0, buffer.length);
    }

    /**
     * discard data from ring.
     * <p> just like read, but not really copy data out.</p>
     * @param len how long would you want to discard
     * @return real discard len
     */
    public int discard(int len) {
        return RingBufferJni.discard(mNativeHandle[0], len);
    }

    /**
     * peek data from ring, but not remove it from ring.
     * <p> just like read, but it's data still on the ring </p>
     * @param buffer your buffer to store data
     * @param offset buffer offset
     * @param len how long would you want to peek
     * @return real peek len
     */
    public int peek(final byte[] buffer, int offset, int len) {
        return RingBufferJni.peek(mNativeHandle[0], buffer, offset, len);
    }

    public int peek(final byte[] buffer) {
        return read(buffer, 0, buffer.length);
    }

    /**
     * clear all data on the ring, NOT THREAD-SAFE!
     * <p> call this method you should ensure ring not in read/write state! </p>
     */
    public void clear() {
        RingBufferJni.clear(mNativeHandle[0]);
    }

    /**
     * close the ring and free it's memory on native.
     * <p>do not use this instance any more after call this!</p>
     */
    @Override
    public void close() {
        if (0 != mNativeHandle[0]) {
            RingBufferJni.destroy(mNativeHandle);
            mNativeHandle[0] = 0;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        // final safety, try save you! do not relay on it, you should close it by yourself!!!
        close();
    }

}
