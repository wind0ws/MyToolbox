package com.threshold.toolbox.log;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * 单元测试 {@link TagFinder}，重点验证匿名内部类能从外部类解析到 TAG。
 */
@LogTag("TagFinderTest")
public class TagFinderTest {

    @After
    public void tearDown() {
        TagFinder.clearCache();
    }

    @Test
    public void directClassReturnsTag() {
        assertEquals("TagFinderTest", TagFinder.findTag(TagFinderTest.class.getName()));
    }

    @Test
    public void anonymousInnerClassReturnsEnclosingTag() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
            }
        };
        String anonymousClassName = r.getClass().getName();
        assertEquals("匿名内部类应使用外部类的 @LogTag",
                "TagFinderTest", TagFinder.findTag(anonymousClassName));
    }

    @Test
    public void unknownClassReturnsEmpty() {
        assertEquals("", TagFinder.findTag("not.a.RealClass"));
    }
}
