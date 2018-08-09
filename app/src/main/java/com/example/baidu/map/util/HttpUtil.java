package com.example.baidu.map.util;

import okhttp3.OkHttpClient;
import okhttp3.Request;
/*
* 接收客户端
* */
public class HttpUtil {
    public static void sendOkHttpRequest(String address,okhttp3.Callback callback){
        OkHttpClient client=new OkHttpClient();
        Request request=new Request.Builder().url(address).build();
        client.newCall(request).enqueue(callback);
    }
}
