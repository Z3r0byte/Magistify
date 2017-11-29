/*
 * Copyright (c) 2016-2017 Bas van den Boom 'Z3r0byte'
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

package net.ilexiconn.magister.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.ilexiconn.magister.Magister;
import net.ilexiconn.magister.adapter.ProfileAdapter;
import net.ilexiconn.magister.adapter.StudyAdapter;
import net.ilexiconn.magister.adapter.SubjectAdapter;
import net.ilexiconn.magister.container.ApiKey;
import net.ilexiconn.magister.container.Profile;
import net.ilexiconn.magister.container.Study;
import net.ilexiconn.magister.container.Subject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import okhttp3.CipherSuite;
import okhttp3.ConnectionSpec;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.TlsVersion;

public class HttpUtil {

    private static ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
            .tlsVersions(TlsVersion.TLS_1_2, TlsVersion.TLS_1_1, TlsVersion.TLS_1_0)
            .cipherSuites(
                    CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
                    CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
                    CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256)
            .build();

    private static OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectionSpecs(Collections.singletonList(spec))
            .cookieJar(new CookieJar() {
                private final HashMap<String, List<Cookie>> cookieStore = new HashMap<>();

                @Override
                public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                    cookieStore.put(url.host(), cookies);
                }

                @Override
                public List<Cookie> loadForRequest(HttpUrl url) {
                    List<Cookie> cookies = cookieStore.get(url.host());
                    return cookies != null ? cookies : new ArrayList<Cookie>();
                }
            })
            .build();
    private static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    public Gson gson = new GsonBuilder()
            .registerTypeAdapter(Profile.class, new ProfileAdapter())
            .registerTypeAdapter(Study[].class, new StudyAdapter())
            .registerTypeAdapter(Subject[].class, new SubjectAdapter())
            .create();
    private static final String TAG = "HttpUtil";
    private static CookieManager cookieManager = new CookieManager();

    public static InputStreamReader httpDelete(String url) throws IOException {
        /*
        HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("DELETE");
        connection.setRequestProperty("Cookie", getCurrentCookies());
        connection.setRequestProperty("X-API-Client-ID", ApiKey.getKey());
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.connect();
        storeCookies(connection);
        return new InputStreamReader(connection.getInputStream());*/

        Request request = new Request.Builder()
                .addHeader("X-API-Client-ID", ApiKey.getKey())
                .url(url)
                .delete()
                .build();
        Response response = okHttpClient.newCall(request).execute();
        return new InputStreamReader(response.body().byteStream());
    }

    public static InputStreamReader httpPut(String url, String json) throws IOException {
        //todo Use OkHTTP
        HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("PUT");
        connection.setRequestProperty("Cookie", getCurrentCookies());
        connection.setRequestProperty("X-API-Client-ID", ApiKey.getKey());
        connection.setRequestProperty("Content-Type", "application/json");
        byte[] data_url = json.getBytes("UTF-8");
        DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
        outputStream.write(data_url);
        outputStream.flush();
        outputStream.close();
        storeCookies(connection);
        return new InputStreamReader(connection.getInputStream());
    }

    public static InputStreamReader httpPost(String url, String data) throws IOException {
        RequestBody body = RequestBody.create(JSON, data);

        Request request = new Request.Builder()
                .addHeader("X-API-Client-ID", ApiKey.getKey())
                .url(url)
                .post(body)
                .build();
        Response response = okHttpClient.newCall(request).execute();
        return new InputStreamReader(response.body().byteStream());
    }

    public static InputStreamReader httpPostRaw(String url, String json) throws IOException {
        HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Cookie", getCurrentCookies());
        connection.setRequestProperty("X-API-Client-ID", ApiKey.getKey());
        connection.setRequestProperty("Content-Type", "application/json");
        byte[] data_url = json.getBytes("UTF-8");
        DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
        outputStream.write(data_url);
        outputStream.flush();
        outputStream.close();
        storeCookies(connection);
        return new InputStreamReader(connection.getInputStream());
    }

    public static InputStreamReader httpPostFile(Magister m, File file) throws IOException {
        //todo Use OkHTTP
        HttpsURLConnection connection = (HttpsURLConnection) new URL(m.school.url + "/api/file").openConnection();

        String boundary = Long.toHexString(System.currentTimeMillis());
        String lineEnd = "\r\n";
        String twoHyphens = "--";

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Cookie", HttpUtil.getCurrentCookies());
        connection.setRequestProperty("X-API-Client-ID", ApiKey.getKey());
        connection.setRequestProperty("Connection", "Keep-Alive");
        connection.setRequestProperty("Cache-Control", "no-cache");
        connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
        connection.setDoOutput(true);
        connection.setUseCaches(false);

        DataOutputStream dos = new DataOutputStream(connection.getOutputStream());
        FileInputStream fis = new FileInputStream(file);

        dos.writeBytes(twoHyphens + boundary + lineEnd);
        dos.writeBytes("Content-Disposition: form-data; name=\"file\";" + " filename=\"" + file.getName() + "\"" + lineEnd);
        dos.writeBytes(lineEnd);

        int bytesAvailable = fis.available();
        int bufferSize = Math.min(bytesAvailable, 1024);
        byte[] buffer = new byte[bufferSize];
        int bytesRead = fis.read(buffer, 0, bufferSize);

        while (bytesRead > 0) {
            dos.write(buffer, 0, bufferSize);
            bytesAvailable = fis.available();
            bufferSize = Math.min(bytesAvailable, 1024);
            bytesRead = fis.read(buffer, 0, bufferSize);
        }

        dos.writeBytes(lineEnd);
        dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

        fis.close();
        dos.flush();
        dos.close();
        if (connection.getResponseCode() >= 200 && connection.getResponseCode() < 400) {
            return new InputStreamReader(connection.getInputStream());
        } else {
            return new InputStreamReader(connection.getErrorStream());
        }
    }

    public static InputStreamReader httpGet(String url) throws IOException {
        Request request = new Request.Builder()
                .addHeader("X-API-Client-ID", ApiKey.getKey())
                .url(url)
                .build();
        Response response = okHttpClient.newCall(request).execute();
        return new InputStreamReader(response.body().byteStream());

    }

    public static File httpGetFile(String url, File downloadDir) throws IOException {
        //todo Use OkHTTP
        HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Cookie", getCurrentCookies());
        if (AndroidUtil.getAndroidSupportCache()) {
            connection.setUseCaches(true);
        }
        connection.setRequestProperty("X-API-Client-ID", ApiKey.getKey());
        String disposition = connection.getHeaderField("Content-Disposition");
        String fileName = disposition.substring(disposition.indexOf("filename=") + 10, disposition.length() - 1);
        File target = new File(downloadDir.getPath() + "\\" + fileName);
        copyFileUsingStream(connection.getInputStream(), target);
        connection.connect();
        storeCookies(connection);
        return target.getAbsoluteFile();
    }

    private static void copyFileUsingStream(InputStream is, File dest) throws IOException {
        if (is == null || dest == null) {
            return;
        }
        OutputStream os = null;
        try {
            os = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            is.close();
            if (os != null) {
                os.close();
            }
        }
    }

    private static void storeCookies(HttpURLConnection connection) {
        Map<String, List<String>> headers = connection.getHeaderFields();
        List<String> cookies = headers.get("Set-Cookie");
        /*if (cookies != null) {
            for (String cookie : cookies) {
                cookieManager.getCookieStore().add(null, HttpCookie.parse(cookie).get(0));
            }
        }*/
        if (cookies != null) {
            cookieManager.getCookieStore().removeAll();
            cookieManager.getCookieStore().add(null, HttpCookie.parse(cookies.get(cookies.size() - 1)).get(0));
        }
    }

    public static String getCurrentCookies() {
        String result = "";
        for (HttpCookie cookie : cookieManager.getCookieStore().getCookies()) {
            result = result.concat(cookie.toString() + ";");
        }
        return result;
    }

    private static String convertToDataString(Map<String, String> data) throws UnsupportedEncodingException {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry entry : data.entrySet()) {
            builder.append(URLEncoder.encode(entry.getKey().toString(), "UTF-8")).append("=").append(URLEncoder.encode(entry.getValue().toString(), "UTF-8")).append("&");
        }
        String result = builder.toString();
        return result.length() > 0 ? result.substring(0, result.length() - 1) : "";
    }

    public static String convertInputStreamReaderToString(InputStreamReader r) throws IOException {
        BufferedReader reader = new BufferedReader(r);
        StringBuilder responseBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            responseBuilder.append(line);
        }
        return responseBuilder.toString();
    }
}