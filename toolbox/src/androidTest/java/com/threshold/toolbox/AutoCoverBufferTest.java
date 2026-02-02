package com.threshold.toolbox;

import static org.junit.Assert.*;

import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Instrumented tests for AutoCoverBuffer behaviors exposed by the Java API.
 *
 * Notes:
 * - These tests assume the native implementation (AutoCoverBufferJni) is available on the device.
 * - Capacities use powers of two so the "alignment to nearest pow of 2" doesn't affect expectations.
 */
@RunWith(AndroidJUnit4.class)
public class AutoCoverBufferTest {

    // ----------- Helpers -----------

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private static String str(byte[] data, int off, int len) {
        return new String(data, off, len, StandardCharsets.UTF_8);
    }

    // ----------- Tests -----------

    @Test(expected = IllegalArgumentException.class)
    public void constructor_illegalSize_throws() {
        //noinspection resource
        new AutoCoverBuffer(1);
    }

    @Test
    public void initial_state_and_simple_write_read() {
        try (AutoCoverBuffer buf = new AutoCoverBuffer(8)) {
            // Initially no data available from readPos 0
            assertEquals(-3, buf.availableReadLen(0));
            assertEquals(0, buf.getWritePos());

            // Write "hello" and verify writePos and available read length
            byte[] src = bytes("hello");
            int w = buf.write(src);
            assertEquals(src.length, w);
            assertEquals(src.length, buf.getWritePos());
            assertEquals(src.length, buf.availableReadLen(0));

            // Read back from readPos 0
            byte[] out = new byte[8];
            Arrays.fill(out, (byte) 0);
            int r = buf.read(0, out, 0, src.length);
            assertEquals(src.length, r);
            assertEquals("hello", str(out, 0, r));
        }
    }

    @Test
    public void write_with_offset_and_read_with_offset() {
        try (AutoCoverBuffer buf = new AutoCoverBuffer(16)) {
            byte[] src = bytes("0123456789");
            // Write "2345" (offset 2, len 4)
            int w = buf.write(src, 2, 4);
            assertEquals(4, w);
            assertEquals(4, buf.getWritePos());

            // Read into buffer with offset
            byte[] out = new byte[10];
            Arrays.fill(out, (byte) '-');
            int r = buf.read(0, out, 3, 4);
            assertEquals(4, r);

            // Expect "---2345---" pattern inside
            assertEquals("---2345---", new String(out, StandardCharsets.UTF_8).substring(0, 10));
        }
    }

    @Test
    public void available_read_len_tracks_progress() {
        try (AutoCoverBuffer buf = new AutoCoverBuffer(8)) {
            assertEquals(-3, buf.availableReadLen(0));

            // After first write
            int w1 = buf.write(bytes("abc"));
            assertEquals(3, w1);
            assertEquals(3, buf.availableReadLen(0));
            assertEquals(3, buf.getWritePos());

            // After second write
            int w2 = buf.write(bytes("def"));
            assertEquals(3, w2);
            assertEquals(6, buf.availableReadLen(0));
            assertEquals(6, buf.getWritePos());

            // Reading from a later readPos reduces available length
            assertEquals(3, buf.availableReadLen(3));
        }
    }

    @Test
    public void wrap_and_auto_cover_keeps_last_capacity_bytes() {
        // Capacity 8; write 5 + 5 = 10 bytes; last 8 should remain.
        try (AutoCoverBuffer buf = new AutoCoverBuffer(8)) {
            int w1 = buf.write(bytes("ABCDE"));      // 5
            assertEquals(5, w1);
            assertEquals(5, buf.getWritePos());

            int w2 = buf.write(bytes("abcde"));      // +5 = 10 total
            assertEquals(5, w2);
            assertEquals(10, buf.getWritePos());

            // The last 8 bytes of "ABCDEabcde" are "CDEabcde"
            String expectedTail = "CDEabcde";

            int readPos = buf.getWritePos() - 8;
            assertTrue("readPos must be non-negative", readPos >= 0);
            assertTrue("availableReadLen should be at least capacity",
                    buf.availableReadLen(readPos) >= 8);

            byte[] out = new byte[8];
            int r = buf.read(readPos, out, 0, out.length);
            assertEquals(8, r);
            assertEquals(expectedTail, str(out, 0, r));
        }
    }

    @Test
    public void read_exact_just_written_region() {
        try (AutoCoverBuffer buf = new AutoCoverBuffer(32)) {
            // Capture readPos before the write to target exactly the region we append.
            int start = buf.getWritePos();
            byte[] payload = bytes("chunk-1");
            int w = buf.write(payload, 0, payload.length);
            assertEquals(payload.length, w);

            byte[] out = new byte[payload.length];
            int r = buf.read(start, out, 0, out.length);
            assertEquals(payload.length, r);
            assertArrayEquals(payload, out);
        }
    }

    @Test
    public void close_is_idempotent() {
        AutoCoverBuffer buf = new AutoCoverBuffer(8);
        buf.close();
        // Calling close again should be a no-op and must not throw.
        buf.close();
    }

    @Test
    public void concurrent_writes_accumulate_and_tail_is_correct() throws Exception {
        // Use capacity 64; write from two threads; final tail (last 64 bytes) should match the concatenation tail.
        final int capacity = 64;
        try (AutoCoverBuffer buf = new AutoCoverBuffer(capacity)) {
            final String a = repeat("A", 50) + repeat("b", 30); // 80 bytes
            final String b = repeat("X", 20) + repeat("y", 60); // 80 bytes

            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(2);

            Thread t1 = new Thread(() -> {
                try {
                    start.await();
                    buf.write(bytes(a));
                } catch (Exception e) {
                    Log.e("AutoCoverBufferTest", "t1 error", e);
                    fail("Thread t1 failed: " + e);
                } finally {
                    done.countDown();
                }
            });

            Thread t2 = new Thread(() -> {
                try {
                    start.await();
                    buf.write(bytes(b));
                } catch (Exception e) {
                    Log.e("AutoCoverBufferTest", "t2 error", e);
                    fail("Thread t2 failed: " + e);
                } finally {
                    done.countDown();
                }
            });

            t1.start();
            t2.start();
            start.countDown();
            assertTrue("writers did not finish", done.await(5, TimeUnit.SECONDS));

            // Total written should equal a.length + b.length
            int totalWritten = a.length() + b.length();
            assertEquals(totalWritten, buf.getWritePos());

            // The buffer should contain the last 'capacity' bytes of (a + b) in write order.
            String concat = a + b;
            String expectedTail = concat.substring(concat.length() - capacity);

            int readPos = buf.getWritePos() - capacity;
            byte[] out = new byte[capacity];
            int r = buf.read(readPos, out, 0, out.length);
            assertEquals(capacity, r);
            assertEquals(expectedTail, str(out, 0, r));
        }
    }

    // ----------- Utility for concurrent test -----------

    private static String repeat(String s, int n) {
        if (n <= 0) return "";
        StringBuilder sb = new StringBuilder(s.length() * n);
        for (int i = 0; i < n; i++) sb.append(s);
        return sb.toString();
    }
}
