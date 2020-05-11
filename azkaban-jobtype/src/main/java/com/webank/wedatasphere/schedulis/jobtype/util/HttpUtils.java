/*
 * Copyright 2020 WeBank
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.webank.wedatasphere.schedulis.jobtype.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.log4j.Logger;

public class HttpUtils {

  private static final Logger logger = Logger.getLogger(HttpUtils.class);

  /**
   * HTTP异步远程执行方法
   * @param actionUrl
   * @param requestBody
   * @return
   */
//  public static String httpUploadHandle(String actionUrl, RequestBody requestBody) throws Exception{
//
//    //设置链接超时 设置写入超时 设置读取超时
//    OkHttpClient okHttpClient = new OkHttpClient.Builder()
//        .connectTimeout(10, TimeUnit.SECONDS)
//        .writeTimeout(20, TimeUnit.SECONDS)
//        .readTimeout(20, TimeUnit.SECONDS)
//        .build();
//
//    Request request = new Request.Builder()
//        .url(actionUrl)
//        .post(requestBody)
//        .build();
//
//    Call call = okHttpClient.newCall(request);
//    String returnData = "";
//    call.enqueue(new Callback() {
//      @Override
//      public void onFailure(Call call, IOException e) {
//        if(e.getCause().equals(SocketTimeoutException.class))
//        {
//          logger.error("异步请求超时");
////          okHttpClient.newCall(call.request()).enqueue(this);
//        }else {
//          e.printStackTrace();
//        }
//        logger.error("异步请求失败");
//      }
//      @Override
//      public void onResponse(Call call, Response response) throws IOException {
//        System.out.println("异步请求成功");
//      }
//    });
//
//    return returnData;
//  }

  /**
   * HTTP同步执行远程方法(base)
   * @param actionUrl
   * @param requestBody
   * @param urlMap
   * @return
   * @throws IOException
   */
  public static Response httpClientHandleBase(String actionUrl, RequestBody requestBody, Map<String, String> urlMap) throws IOException {
    String maskUrl = actionUrl + "appid=" + urlMap.get("appid") + "&&nonce=" + urlMap.get("nonce")
            +  "&&timestamp=" + urlMap.get("timestamp") + "&&signature=" + urlMap.get("signature");
    //设置链接超时 设置写入超时 设置读取超时
    OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build();

    logger.info("access mask URL is:"+maskUrl);
    Request request = new Request.Builder()
            .url(maskUrl)
            .post(requestBody)
            .build();
    Call call = okHttpClient.newCall(request);
    Response response = call.execute();
    logger.info("mask interface response code：" + response.code());
    return response;
  }

  /**
   * HTTP同步远程执行方法
   * @param actionUrl
   * @param requestBody
   * @return
   */
  public static String httpClientHandle(String actionUrl, RequestBody requestBody, Map<String, String> urlMap) throws Exception{
    String returnData = "";
    try {
      Response response = httpClientHandleBase(actionUrl, requestBody, urlMap);
      returnData = response.body().string();
      logger.info("mask interface return message：" + returnData);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return returnData;
  }

  /**
   * HTTP同步远程执行方法
   * @param actionUrl
   * @return
   */
  public static String httpClientHandle(String actionUrl) throws Exception{

    //设置链接超时 设置写入超时 设置读取超时
    OkHttpClient okHttpClient = new OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build();

    Request request = new Request.Builder()
        .url(actionUrl)
        .build();

    Call call = okHttpClient.newCall(request);
    String returnData = "";
    try {
      Response response = call.execute();
      returnData = response.body().string();
      logger.info("interface return message：" + returnData);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return returnData;
  }

  public static Map<String, String> getReturnMap(String dataStr){
    Map<String, String> dataMap = new HashMap<>();
    GsonBuilder gb = new GsonBuilder();
    Gson g = gb.create();
    dataMap = g.fromJson(dataStr, new TypeToken<Map<String, String>>(){}.getType());
    return dataMap;
  }

  /**
   * 初始化ESB 接口校验参数
   * @return
   */
  public static Map<String, String> initSelectParams(Properties props){
    String appid = props.getProperty(DataChecker.MASK_APP_ID);
    String token = props.getProperty(DataChecker.MASK_APP_TOKEN);

    String nonce = RandomStringUtils.random(5, "0123456789");

    Long cur_time = System.currentTimeMillis() / 1000;

    Map<String, String> requestProperties = new HashMap<>();
    requestProperties.put("appid", appid);
    requestProperties.put("nonce", nonce.toString());
    requestProperties.put("signature", getMD5(getMD5(appid + nonce.toString() + cur_time) + token));
    requestProperties.put("timestamp", cur_time.toString());

    return requestProperties;
  }

  public static String getMD5(String str){

    return DigestUtils.md5Hex(str.getBytes());
  }

  public static void main(String[] args) {

    Map resultMap = new HashMap();
    String maskUrl = "http://10.255.10.90:8087/api/v1/mask-status?";

    RequestBody requestBody = new FormBody.Builder()
        .add("targetDb", "bdp_test_ods_mask")
        .add("targetTable", "ccpd_dump")
        .add("partition", "dcn_id=UA0/type_id=6042/ip=10.240.228.31/ds=2015-04-06")
        .build();

    FormBody.Builder formBuilder = new FormBody.Builder();

    Map<String, String> map = new HashMap<>();
    map.put("targetDb", "bdp_test_ods_mask");
    map.put("targetTable", "ccpd_dump");
    //map.put("partition", "ds=20180925");
    map.put("partition", "ds\\=20180925");

    //String params = gson.toJson(map);
    MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    Iterator<String> iterator = map.keySet().iterator();
    String key = "";
    while(iterator.hasNext()){
      key = iterator.next().toString();
      formBuilder.add(key, map.get(key));
    }

    RequestBody requestBody2 = formBuilder.build();

    Properties props = new Properties();
    props.put(DataChecker.MASK_APP_ID, "wtss");
    props.put(DataChecker.MASK_APP_TOKEN, "20a0ccdfc0");

    Map<String, String> dataMap = HttpUtils.initSelectParams(props);

    try {
      String result = HttpUtils.httpClientHandle(maskUrl, requestBody, dataMap);
      System.out.println(result);
      Map resulMap = HttpUtils.getReturnMap(result);
      if("200".equals(resulMap.get("code"))){
        System.out.println("数据查找成功变更datacheck状态");
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println(e.getMessage());
    }

  }

}
