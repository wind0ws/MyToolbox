package com.threshold.toolbox;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

public class JsonTest {

    private void output(String json) throws JSONException {
        JSONObject jsonObject1 = new JSONObject(json);
        final String rewakeEnableStr = jsonObject1.optString("rewake_enable", null);
        final boolean rewakeEnableBoolean = jsonObject1.optBoolean("rewake_enable", false);
        System.out.println("rewakeEnableStr=" + rewakeEnableStr + ",rewakeEnableBoolean=" + rewakeEnableBoolean);
    }

    @Test
    public void test() throws JSONException {
        String json1 = "{\"rewake_enable\":\"2\"}";
        String json2 = "{\"rewake_enable\":true}";
        output(json1);
        System.out.println("=================================");
        output(json2);
    }
}
