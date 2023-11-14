package com.threshold.toolbox;

import com.threshold.jni.ToolboxJni;

/**
 * One Consumer(read) One Producer(write) Thread-Safe RingBuffer.
 *
 * <p> not all of method are thread-safe(such as Clear),
 * only safe in this condition: 1 consumer and 1 producer </p>
 */
public class RingBuffer {

    // store ring buffer native handle: why use long, for compat with 64bit OS
    private final long[] mNativeHandle = new long[]{0};
    // store ring buffer can read data len
    private final int[] mReceiveDataLen = new int[]{0};
    // store ring buffer can write data len
    private final int[] mReceiveFreeSpace = new int[]{0};

    public RingBuffer(int bufferSize) {
        int ret = ToolboxJni.rbufCreate(mNativeHandle, bufferSize);
        if (0 != ret || 0 == mNativeHandle[0]) {
            throw new IllegalArgumentException(
                    String.format("failed on create ring buffer. bufferSize=%d", bufferSize));
        }
    }

    public int availableReadLen() {
        ToolboxJni.rbufAvailableRead(mNativeHandle, mReceiveDataLen);
        return mReceiveDataLen[0];
    }

    public int availableWriteLen() {
        ToolboxJni.rbufAvailableWrite(mNativeHandle, mReceiveFreeSpace);
        return mReceiveFreeSpace[0];
    }

    public boolean isEmpty() {
       return availableReadLen() < 1;
    }

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
        return ToolboxJni.rbufWrite(mNativeHandle, data, offset, len);
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
        return ToolboxJni.rbufRead(mNativeHandle, buffer, offset, len);
    }

    public int read(final byte[] buffer) {
        return read(buffer, 0, buffer.length);
    }

    public int discard(int len) {
        return ToolboxJni.rbufDiscard(mNativeHandle, len);
    }

    public int peek(final byte[] buffer, int offset, int len) {
        return ToolboxJni.rbufPeek(mNativeHandle, buffer, offset, len);
    }

    public int peek(final byte[] buffer) {
        return read(buffer, 0, buffer.length);
    }

    /**
     * clear all data, not thread-safe!
     * call this method you should ensure not in read/write state
     */
    public void clear() {
        ToolboxJni.rbufClear(mNativeHandle);
    }

    public void close() {
        if (mNativeHandle[0] != 0) {
            ToolboxJni.rbufDestroy(mNativeHandle);
            mNativeHandle[0] = 0;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        close();
    }

}
