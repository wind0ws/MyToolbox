package com.threshold.jni;

public class AutoCoverBufferJni {

    static {
        System.loadLibrary("toolbox");
    }


    //========================== AutoCoverBuffer start ==========================

    /**
     * create auto cover buffer.
     *
     * @param handle_holder handle
     * @param buffer_size   buffer size you want to create
     * @return 0 for success
     */
    public static native int create(long[] handle_holder, int buffer_size);

    /**
     * available read data len from ring
     *
     * @param handle   handle
     * @param read_pos start read position
     * @return available data len
     */
    public static native int availableRead(long handle, int read_pos);

    /**
     * read specified len byte from auto cover buffer by read_position and copy it to data from offset
     *
     * @param handle   handle
     * @param read_pos start read position
     * @param data     copy read out bytes to data
     * @param offset   data offset
     * @param len      how long you want to copy to data
     * @return real read len
     */
    public static native int read(long handle, int read_pos, byte[] data, int offset, int len);

    /**
     * write specified len byte from data's offset to auto cover buffer tail.
     * if current ring is full, we will drop some head data for place these data to buffer tail.
     *
     * @param handle handle
     * @param data   data will copied to auto cover buffer
     * @param offset data offset
     * @param len    how long you want write to auto cover buffer
     * @return real write len
     */
    public static native int write(long handle, byte[] data, int offset, int len);

    /**
     * destroy the auto cover buffer
     *
     * @param handle_holder handle
     * @return 0 for success
     */
    public static native int destroy(long[] handle_holder);


    //========================== AutoCoverBuffer end ==========================


}
