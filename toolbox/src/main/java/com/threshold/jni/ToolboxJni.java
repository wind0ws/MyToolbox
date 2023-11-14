package com.threshold.jni;

public class ToolboxJni {

    static {
        System.loadLibrary("toolbox");
    }

    //========================== Ring buffer start ==========================

    /**
     * create ring buffer( FIFO buffer ).
     * @param handle_holder handle
     * @param buffer_size buffer size you want to create
     * @return 0 for success
     */
    public static native int rbufCreate(long[] handle_holder, int buffer_size);

    /**
     * available read data len from ring
     * @param handle_holder handle
     * @param data_len_holder int array for receive data len
     * @return 0 for success
     */
    public static native int rbufAvailableRead(long[] handle_holder, int[] data_len_holder);

    /**
     * available write len to ring. (free space to write)
     * @param handle_holder handle
     * @param data_len_holder int array for receive free space
     * @return 0 for success
     */
    public static native int rbufAvailableWrite(long[] handle_holder, int[] data_len_holder);

    /**
     * discard specified len data from ring buffer.
     * just like read, but not really copied data out.
     * @param handle_holder handle
     * @param len how long that you want to discard
     * @return real discard len
     */
    public static native int rbufDiscard(long[] handle_holder, int len);

    /**
     * peek data from ring buffer
     * just like read, but do not remove it from queue
     * @param handle_holder handle
     * @param data copy read out bytes to data
     * @param offset data offset
     * @param len how long you want to copy to data
     * @return real peek data len
     */
    public static native int rbufPeek(long[] handle_holder, byte[] data, int offset, int len);

    /**
     * read specified len byte from ring buffer and copy it to data from offset
     * @param handle_holder handle
     * @param data copy read out bytes to data
     * @param offset data offset
     * @param len how long you want to copy to data
     * @return real read len
     */
    public static native int rbufRead(long[] handle_holder, byte[] data, int offset, int len);

    /**
     * write specified len byte from data's offset to ring buffer
     * @param handle_holder handle
     * @param data data will copied to ring buffer
     * @param offset data offset
     * @param len how long you want write to ring buffer
     * @return real write len
     */
    public static native int rbufWrite(long[] handle_holder, byte[] data, int offset, int len);

    /**
     * clear all data, NOT THREAD SAFE
     * call this method you should ensure not in read/write state
     * @param handle_holder handle
     * @return 0
     */
    public static native int rbufClear(long[] handle_holder);

    /**
     * destroy the ring buffer
     * @param handle_holder handle
     * @return 0 for success
     */
    public static native int rbufDestroy(long[] handle_holder);

    //========================== Ring buffer end ==========================

}
