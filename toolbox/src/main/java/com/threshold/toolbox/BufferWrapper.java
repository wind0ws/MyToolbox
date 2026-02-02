package com.threshold.toolbox;

import androidx.annotation.Keep;

/**
 * a buffer wrapper
 */
@Keep
public class BufferWrapper {
    final byte[] buffer;
    // current bytes in use.
    int bufferUsed;

    public BufferWrapper(int bufferCapacity) {
        if (bufferCapacity < 1) {
            throw new IllegalArgumentException("bad buffer.capacity");
        }
        this.buffer = new byte[bufferCapacity];
    }

    public BufferWrapper(final byte[] buffer, final int bufferUsed) {
        this.buffer = buffer;
        setBufferUsed(bufferUsed);
    }

    public byte[] getBuffer() {
        return buffer;
    }

    public int getBufferUsed() {
        return bufferUsed;
    }

    public int getCapacity() {
        return this.buffer.length;
    }

    public void setBufferUsed(final int bufferUsed) {
        if (bufferUsed > this.buffer.length) {
            throw new IllegalArgumentException(
                    String.format("buffer.capacity=%d, but bufferUsed=%d",
                            this.buffer.length, bufferUsed));
        }
        this.bufferUsed = bufferUsed;
    }
}