package com.shencoder.javacv_facedetectdemo;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ShenBen
 * @date 2019/8/21 14:13
 * @email 714081644@qq.com
 */
public class GsonUtil {

    private static Gson sGson;

    static {
        sGson = new GsonBuilder().serializeNulls().setLenient().create();
    }

    public static String toJson(Object object) {
        return sGson.toJson(object);
    }

    public static <T> T jsonToBean(String json, Class<T> tClass) {
        return sGson.fromJson(json, tClass);
    }

    public static <T> T jsonToBean(String json, Type typeOfT) {
        return sGson.fromJson(json, typeOfT);
    }

    /**
     * json字符串转成list
     *
     * @param json
     * @param cls
     * @return
     */
    public static <T> List<T> jsonToList(String json, Class<T> cls) {
        List<T> list = new ArrayList<>();
        if (TextUtils.isEmpty(json)) {
            return list;
        }
        JsonArray array = new JsonParser().parse(json).getAsJsonArray();
        for (final JsonElement elem : array) {
            list.add(sGson.fromJson(elem, cls));
        }
        return list;
    }

}
