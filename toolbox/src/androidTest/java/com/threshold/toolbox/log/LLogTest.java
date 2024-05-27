package com.threshold.toolbox.log;

import android.support.annotation.NonNull;
import android.support.test.runner.AndroidJUnit4;

import com.threshold.toolbox.log.llog.LLog;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
@LogTag("LogTest")
public class LLogTest {

    private static class UserInfo4Test {
        private final String name;
        private final int age;

        UserInfo4Test(final String name, final int age) {
            this.name = name;
            this.age = age;
        }

        @NonNull
        @Override
        public String toString() {
            return "User{" +
                    "name='" + name + '\'' +
                    ", age=" + age +
                    '}';
        }
    }

    @Test
    public void test() {
        LLog.v("hello verbose");
        LLog.vWithTag("UserTag", "hello verbose with UserTag");

        final List<String> names = new ArrayList<>(8);
        names.add("zhang san");
        names.add("li si");
        names.add("wang wu");
        LLog.obj(names);

        final UserInfo4Test alice = new UserInfo4Test("alice", 18);
        LLog.obj(alice);

        final Map<String, Integer> students = new HashMap<>(8);
        students.put("michael", 8);
        students.put("jane", 10);
        students.put("tom", 12);
        students.put("jerry", 14);
        LLog.obj(students);

        LLog.json("{ \"keyword\":\"hello\", \"id\": 0 }");
    }

}
