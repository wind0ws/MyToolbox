package com.threshold.toolbox;

import android.os.SystemClock;
import android.support.test.runner.AndroidJUnit4;

import com.threshold.toolbox.log.llog.LLog;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class RingBufferTest {

    private static class Flag {
        private boolean running = true;
    }

    @Test
    public void test() throws InterruptedException {
        final RingBuffer ringBuffer = new RingBuffer(1024);
        Assert.assertEquals(ringBuffer.availableReadLen(), 0);
        LLog.i("ringBuffer.availableWriteLen()=%d", ringBuffer.availableWriteLen());
        Assert.assertEquals(ringBuffer.availableWriteLen(), 1024);

        final Flag flag = new Flag();

        final byte[] expectedBuffer = new byte[]{0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6,
                0x7, 0x8, 0x9, 0xA, 0xB, 0xC, 0xD, 0xE, 0xF};

        Thread consumer = new Thread(new Runnable() {
            final byte[] buffer = new byte[expectedBuffer.length];

            @Override
            public void run() {
                while (flag.running) {
                    if (ringBuffer.availableReadLen() < buffer.length) {
                        SystemClock.sleep(2);
                        continue;
                    }
                    ringBuffer.read(buffer);
                    Assert.assertArrayEquals(buffer, expectedBuffer);
                }
            }
        });

        Thread producer = new Thread(new Runnable() {
            @Override
            public void run() {
                while (flag.running) {
                    if (ringBuffer.availableWriteLen() < 4) {
                        SystemClock.sleep(1);
                        continue;
                    }
                    ringBuffer.write(expectedBuffer);
                }
            }
        });
        consumer.start();
        producer.start();
        LLog.d("consumer and producer started");

        SystemClock.sleep(20000);
        flag.running = false;

        LLog.i("--> join consumer/producer");
        consumer.join();
        producer.join();
        LLog.i("<-- join consumer/producer complete");

        ringBuffer.close();
        LLog.i("bye bye...");
    }

}
