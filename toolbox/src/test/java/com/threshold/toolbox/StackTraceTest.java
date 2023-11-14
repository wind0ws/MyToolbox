package com.threshold.toolbox;

import org.junit.Test;

public class StackTraceTest {

    @Test
    public void test() {
        final StackTraceElement[] stackTraces = Thread.currentThread().getStackTrace();
        final StackTraceElement stackTrace = stackTraces[1];

        System.out.println("className = " + stackTrace.getClassName());
        for (int i = 0; i < stackTraces.length; i++) {
            System.out.println("stack[" + i + "]=>");
            System.out.println(stackTraces[i]);
            System.out.println("\n\n");
        }
    }

}
