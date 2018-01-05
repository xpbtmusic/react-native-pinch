package com.localz.pinch.utils;

import android.text.TextUtils;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.WritableMap;

import com.localz.pinch.models.HttpRequest;
import com.localz.pinch.models.HttpResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class HttpUtil {
    private static final String DEFAULT_CONTENT_TYPE = "application/json";
    private static final String BOUNDARY = java.util.UUID.randomUUID().toString();
    private static final String TWO_HYPHENS = "--";
    private static final String LINE_END = "\r\n";

    public static final String FILE_TYPE_FILE = "file/*";
    public static final String FILE_TYPE_IMAGE = "image/*";
    public static final String FILE_TYPE_AUDIO = "audio/*";
    public static final String FILE_TYPE_VIDEO = "video/*";

    private String getResponseBody(InputStream responseStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(responseStream));
        StringBuilder sb = new StringBuilder();
        String line;

        while ((line = bufferedReader.readLine()) != null) {
            sb.append(line);
        }
        bufferedReader.close();

        return sb.toString();
    }

    private WritableMap getResponseHeaders(HttpsURLConnection connection) {
        WritableMap jsonHeaders = Arguments.createMap();
        Map<String, List<String>> headerMap = connection.getHeaderFields();

        for (Map.Entry<String, List<String>> entry : headerMap.entrySet()) {
            if (entry.getKey() != null) {
                jsonHeaders.putString(entry.getKey(), entry.getValue().get(0));
            }
        }

        return jsonHeaders;
    }

    private WritableMap getResponseHeaders(HttpURLConnection connection) {
        WritableMap jsonHeaders = Arguments.createMap();
        Map<String, List<String>> headerMap = connection.getHeaderFields();

        for (Map.Entry<String, List<String>> entry : headerMap.entrySet()) {
            if (entry.getKey() != null) {
                jsonHeaders.putString(entry.getKey(), entry.getValue().get(0));
            }
        }

        return jsonHeaders;
    }

    private HttpsURLConnection prepareRequestHeaders(HttpsURLConnection connection, JSONObject headers) throws JSONException {
        connection.setRequestProperty("Content-Type", DEFAULT_CONTENT_TYPE);
        connection.setRequestProperty("Accept", DEFAULT_CONTENT_TYPE);

        if (headers != null) {
            Iterator<String> iterator = headers.keys();
            while (iterator.hasNext()) {
                String nextKey = iterator.next();
                Log.e("headers", " key " + nextKey + "---" + headers.get(nextKey).toString());
                if (!TextUtils.isEmpty(nextKey) && nextKey.toLowerCase().equals("authorization")) {
                    connection.setRequestProperty(nextKey, "Token " + headers.get(nextKey).toString());
                } else {
                    connection.setRequestProperty(nextKey, headers.get(nextKey).toString());
                }
            }
        }

        return connection;
    }

    private HttpURLConnection prepareRequestHeaders(HttpURLConnection connection, JSONObject headers) throws JSONException {
        connection.setRequestProperty("Content-Type", DEFAULT_CONTENT_TYPE);
        connection.setRequestProperty("Accept", DEFAULT_CONTENT_TYPE);

        if (headers != null) {
            Iterator<String> iterator = headers.keys();
            while (iterator.hasNext()) {
                String nextKey = iterator.next();
                connection.setRequestProperty(nextKey, headers.get(nextKey).toString());
            }
        }

        return connection;
    }

    private HttpsURLConnection prepareRequest(HttpRequest request)
            throws IOException, KeyStoreException, CertificateException, KeyManagementException, NoSuchAlgorithmException, JSONException {
        HttpsURLConnection connection;
        URL url = new URL(request.endpoint);
        String method = request.method.toUpperCase();

        connection = (HttpsURLConnection) url.openConnection();
        if (request.certFilenames != null) {
            connection.setSSLSocketFactory(KeyPinStoreUtil.getInstance(request.certFilenames).getContext().getSocketFactory());
        }
        connection.setRequestMethod(method);

        connection = prepareRequestHeaders(connection, request.headers);

        connection.setRequestProperty("Accept-Charset", "UTF-8");
        connection.setAllowUserInteraction(false);
        connection.setConnectTimeout(request.timeout);
        connection.setReadTimeout(request.timeout);

        if ((method.equals("POST") || method.equals("PUT") || method.equals("DELETE"))) {
            // Set the content length of the body.
            Log.e("request body", request.body);
            if (request.files == null&&!TextUtils.isEmpty(request.body)) {
                connection.setRequestProperty("Content-length", request.body.getBytes("UTF-8").length + "");
            }
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);

            if (request.files != null) {
                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.setRequestProperty("Charset", "UTF-8");
                connection.setRequestProperty("Content-Type", "multipart/form-data; BOUNDARY=" + BOUNDARY);
                DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
                outputStream.write(getParamsString(request.body).getBytes("UTF-8"));//上传参数
                outputStream.flush();

                ArrayList<File> files = request.files;
                int filesCount = files.size();
                for (int i = 0; i < filesCount; i++) {
                    writeFile(files.get(i), "image", FILE_TYPE_FILE, outputStream);
                }

                byte[] endData = (LINE_END + TWO_HYPHENS + BOUNDARY + TWO_HYPHENS + LINE_END).getBytes();//写结束标记位
                outputStream.write(endData);
                outputStream.flush();
            } else {

                // Send the JSON as body of the request.
                OutputStream outputStream = connection.getOutputStream();
                outputStream.write(request.body.getBytes("UTF-8"));
                outputStream.close();
            }
        }

        return connection;
    }

    /**
     * 上传文件时得到一定格式的拼接字符串
     */
    private String getFileParamsString(File file, String fileKey, String fileType) {
        StringBuffer strBuf = new StringBuffer();
        strBuf.append(LINE_END);
        strBuf.append(TWO_HYPHENS);
        strBuf.append(BOUNDARY);
        strBuf.append(LINE_END);
        strBuf.append("Content-Disposition: form-data; name=\"" + fileKey + "\"; filename=\"" + file.getName() + "\"");
        strBuf.append(LINE_END);
        strBuf.append("Content-Type: " + fileType);
        strBuf.append(LINE_END);
        strBuf.append("Content-Lenght: " + file.length());
        strBuf.append(LINE_END);
        strBuf.append(LINE_END);
        return strBuf.toString();
    }

    /**
     * 上传文件时得到拼接的参数字符串
     */
    private String getParamsString(String paramsMap) throws JSONException {
        if(TextUtils.isEmpty(paramsMap)){
            return null;
        }
        JSONObject jsonObject=new JSONObject(paramsMap);
        StringBuffer strBuf = new StringBuffer();
        JSONArray jsonArray = jsonObject.names();
        for (int i = 0; i < jsonArray.length(); i++) {
            String key = (String) jsonArray.get(i);
            String value = jsonObject.getString(key);
            Log.e("jsonObject ---- ", key + "---- " + value+"---"+value.length());
            strBuf.append(TWO_HYPHENS);
            strBuf.append(BOUNDARY);
            strBuf.append(LINE_END);
            strBuf.append("Content-Disposition: form-data; name=\"" + key + "\"");
            strBuf.append(LINE_END);

            strBuf.append("Content-Type: " + "text/plain");
            strBuf.append(LINE_END);
            strBuf.append("Content-Lenght: " + value.length());
            strBuf.append(LINE_END);
            strBuf.append(LINE_END);
            strBuf.append(value);
            strBuf.append(LINE_END);
        }
        return strBuf.toString();
    }

    /**
     * 上传文件时写文件
     */
    private void writeFile(File file, String fileKey, String fileType, DataOutputStream outputStream) throws IOException {
        outputStream.write(getFileParamsString(file, fileKey, fileType).getBytes());
        outputStream.flush();

        FileInputStream inputStream = new FileInputStream(file);
        final long total = file.length();
        long sum = 0;
        byte[] buffer = new byte[1024 * 2];
        int length = -1;
        while ((length = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, length);
            sum = sum + length;
            //if(callBack != null){
            final long finalSum = sum;
              /*  CallBackUtil.mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callBack.onProgress(finalSum * 100.0f / total,total);
                    }
                });*/
            // }
        }
        outputStream.flush();
        inputStream.close();
    }

    private HttpURLConnection prepareRequestForHttp(HttpRequest request)
            throws IOException, KeyStoreException, CertificateException, KeyManagementException, NoSuchAlgorithmException, JSONException {
        HttpURLConnection connection;
        URL url = new URL(request.endpoint);
        String method = request.method.toUpperCase();

        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);

        connection = prepareRequestHeaders(connection, request.headers);

        connection.setRequestProperty("Accept-Charset", "UTF-8");
        connection.setAllowUserInteraction(false);
        connection.setConnectTimeout(request.timeout);
        connection.setReadTimeout(request.timeout);

        if ((method.equals("POST") || method.equals("PUT") || method.equals("DELETE"))) {
            // Set the content length of the body.
            Log.e("request body", request.body);
            if (request.files == null&&!TextUtils.isEmpty(request.body)) {
                connection.setRequestProperty("Content-length", request.body.getBytes("UTF-8").length + "");
            }
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);

            if (request.files != null) {
                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.setRequestProperty("Charset", "UTF-8");
                connection.setRequestProperty("Content-Type", "multipart/form-data; BOUNDARY=" + BOUNDARY);
                DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
                outputStream.write(getParamsString(request.body).getBytes("UTF-8"));//上传参数
                outputStream.flush();

                ArrayList<File> files = request.files;
                int filesCount = files.size();
                for (int i = 0; i < filesCount; i++) {
                    writeFile(files.get(i), "image", FILE_TYPE_FILE, outputStream);
                }

                byte[] endData = (LINE_END + TWO_HYPHENS + BOUNDARY + TWO_HYPHENS + LINE_END).getBytes();//写结束标记位
                outputStream.write(endData);
                outputStream.flush();
            } else {

                // Send the JSON as body of the request.
                OutputStream outputStream = connection.getOutputStream();
                outputStream.write(request.body.getBytes("UTF-8"));
                outputStream.close();
            }
        }

        return connection;
    }

    private InputStream prepareResponseStream(HttpsURLConnection connection) throws IOException {
        try {
            return connection.getInputStream();
        } catch (IOException e) {
            return connection.getErrorStream();
        }
    }

    private InputStream prepareResponseStream(HttpURLConnection connection) throws IOException {
        try {
            return connection.getInputStream();
        } catch (IOException e) {
            return connection.getErrorStream();
        }
    }

    public HttpResponse sendHttpRequest(HttpRequest request)
            throws IOException, KeyStoreException, CertificateException, KeyManagementException, NoSuchAlgorithmException, JSONException {
        InputStream responseStream = null;
        HttpResponse response = new HttpResponse();
        HttpsURLConnection connection;
        int status;
        String statusText;

        try {
            connection = prepareRequest(request);

            connection.connect();

            status = connection.getResponseCode();
            statusText = connection.getResponseMessage();
            responseStream = prepareResponseStream(connection);

            response.statusCode = status;
            response.statusText = statusText;
            response.bodyString = getResponseBody(responseStream);
            response.headers = getResponseHeaders(connection);

            return response;
        } finally {
            if (responseStream != null) {
                responseStream.close();
            }
        }
    }

    public HttpResponse sendHttpRequestForHttp(HttpRequest request)
            throws IOException, KeyStoreException, CertificateException, KeyManagementException, NoSuchAlgorithmException, JSONException {
        InputStream responseStream = null;
        HttpResponse response = new HttpResponse();
        HttpURLConnection connection;
        int status;
        String statusText;

        try {
            connection = prepareRequestForHttp(request);

            connection.connect();

            status = connection.getResponseCode();
            statusText = connection.getResponseMessage();
            responseStream = prepareResponseStream(connection);

            response.statusCode = status;
            response.statusText = statusText;
            response.bodyString = getResponseBody(responseStream);
            response.headers = getResponseHeaders(connection);

            return response;
        } finally {
            if (responseStream != null) {
                responseStream.close();
            }
        }
    }

}
