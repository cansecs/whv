package com.example.chenches.fullscreen;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.Buffer;

/**
 * Created by chenches on 8/25/2016.
 */
public class loadbalance {

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public String getContent(URL url){
        String content="";
        if ( url == null) return null;
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            String contentType = connection.getHeaderField("Content-Type");
            String charset = null;
            if ( contentType != null ) {
                for (String param : contentType.replace(" ", "").split(";")) {
                    if (param.startsWith("charset=")) {
                        charset = param.split("=", 2)[1];
                        break;
                    }
                }
                if (charset != null) {
                    InputStream is = null;
                    try {
                        is = connection.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(is, charset));
                        content = reader.readLine();
                        if (is != null) {
                            is.close();
                        }
                    } finally {
                        if (is != null) {
                            is.close();
                        }
                    }
                }
            }
            Log.d("Content",String.format("%s:%s",url,content));
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if ( connection !=null){
                connection.disconnect();
            }
        }
        return content;
    }

    public boolean isGood(String hay,String needle) {
        return hay != null && needle != null && hay.startsWith(needle);
    }
    public String[] chooseCandidates(String[] urls, String goodcontent){
        String[] ret=(String[])null;
        try {
            int i;
            for ( i=0; i < urls.length; i++) {
                URL url=new URL(urls[i]);
                String content=getContent(url);
                if (isGood(content, goodcontent)) {
                    String host=url.getHost();
                    ret=new String[]{host,content};
                    break;
                }
            }
        } catch (MalformedURLException e) {
            Log.d("Unkown URL",String.valueOf(e));
        }
        return ret;
    }
}
