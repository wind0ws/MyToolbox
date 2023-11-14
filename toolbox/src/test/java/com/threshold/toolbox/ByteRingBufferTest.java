package com.threshold.toolbox;

import org.junit.Test;

import java.util.Random;

public class ByteRingBufferTest {
    private static final int maxSize = 256;

    private static ByteRingBuffer ringBuffer;
    private static Random random;
    private static long txCounter;          // number of bytes written into ring buffer
    private static long rxCounter;          // number of bytes read from ring buffer
    private static byte[] buf;

    @Test
    public void runTest() {
        ringBuffer = new ByteRingBuffer(maxSize);
        random = new Random(123);
        buf = new byte[maxSize];
        for (int ctr = 0; ctr < 100000000; ctr++) {
            if (ctr % 100000 == 0) {
                System.out.print(".");
            }
            performRandomOperation();
            checkRingBuffer();
        }
    }

    private static void checkRingBuffer() {
        if (ringBuffer.getUsed() != txCounter - rxCounter) {
            throw new AssertionError();
        }
    }

    private static void performRandomOperation() {
        int r = random.nextInt(1000);
        if (r == 0) {                                           // resize
            int newSize = 1 + random.nextInt(maxSize);
            ringBuffer.resize(newSize);
            rxCounter = Math.max(rxCounter, txCounter - newSize);
        } else if (r == 1) {                                     // clear
            ringBuffer.clear();
            rxCounter = txCounter;
        } else if (r <= 5) {                                     // discard
            int len = random.nextInt(maxSize + 1);
            ringBuffer.discard(len);
            rxCounter = Math.min(rxCounter + len, txCounter);
        } else if (r <= 500) {                                   // read
            int len = random.nextInt(maxSize + 1);
            int pos = random.nextInt(maxSize - len + 1);
            int expectedTrLen = Math.min(len, ringBuffer.getUsed());
            int trLen = ringBuffer.read(buf, pos, len);
            if (trLen != expectedTrLen) {
                throw new AssertionError();
            }
            verifyData(buf, pos, trLen, rxCounter);
            rxCounter += trLen;
        } else {                                                 // write
            int len = random.nextInt(maxSize + 1);
            int pos = random.nextInt(maxSize - len + 1);
            generateData(buf, pos, len, txCounter);
            int expectedTrLen = Math.min(len, ringBuffer.getFree());
            int trLen = ringBuffer.write(buf, pos, len);
            if (trLen != expectedTrLen) {
                throw new AssertionError();
            }
            txCounter += trLen;
        }
    }

    private static void verifyData(byte[] buf, int pos, int len, long ctr) {
        for (int p = 0; p < len; p++) {
            int v1 = (int) ((ctr + p) % 256);
            int v2 = buf[pos + p] & 0xff;
            if (v1 != v2) {
                throw new AssertionError();
            }
        }
    }

    private static void generateData(byte[] buf, int pos, int len, long ctr) {
        for (int p = 0; p < len; p++) {
            int v = (int) ((ctr + p) % 256);
            buf[pos + p] = (byte) v;
        }
    }
}
