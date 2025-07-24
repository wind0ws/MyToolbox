package com.threshold.jni;

public class RingBufferJni {

    static {
        System.loadLibrary("toolbox");
    }


    //========================== Ring buffer start ==========================

    /**
     * create ring buffer( FIFO buffer ).
     *
     * @param handle_holder handle
     * @param buffer_size   buffer size you want to create
     * @return 0 for success
     */
    public static native int create(long[] handle_holder, int buffer_size);

    /**
     * available read data len from ring
     *
     * @param handle handle
     * @return available data len
     */
    public static native int availableRead(long handle);

    /**
     * available write len to ring. (free space to write)
     *
     * @param handle handle
     * @return available write len
     */
    public static native int availableWrite(long handle);

    /**
     * discard specified len data from ring buffer.
     * just like read, but not really copied data out.
     *
     * @param handle handle
     * @param len    how long that you want to discard
     * @return real discard len
     */
    public static native int discard(long handle, int len);

    /**
     * peek data from ring buffer
     * just like read, but do not remove it from queue
     *
     * @param handle handle
     * @param data   copy read out bytes to data
     * @param offset data offset
     * @param len    how long you want to copy to data
     * @return real peek data len
     */
    public static native int peek(long handle, byte[] data, int offset, int len);

    /**
     * read specified len byte from ring buffer and copy it to data from offset
     *
     * @param handle handle
     * @param data   copy read out bytes to data
     * @param offset data offset
     * @param len    how long you want to copy to data
     * @return real read len
     */
    public static native int read(long handle, byte[] data, int offset, int len);

    /**
     * write specified len byte from data's offset to ring buffer
     *
     * @param handle handle
     * @param data   data will copied to ring buffer
     * @param offset data offset
     * @param len    how long you want write to ring buffer
     * @return real write len
     */
    public static native int write(long handle, byte[] data, int offset, int len);

    /**
     * clear all data, NOT THREAD SAFE
     * call this method you should ensure not in read/write state
     *
     * @param handle handle
     * @return 0
     */
    public static native int clear(long handle);

    /**
     * destroy the ring buffer
     *
     * @param handle_holder handle
     * @return 0 for success
     */
    public static native int destroy(long[] handle_holder);


    //========================== Ring buffer end ==========================


}
