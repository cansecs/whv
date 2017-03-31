package com.cansecs.workholes;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
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

    final static String pathDeli = "__";
    public static String cacheDir = "";
    protected Activity activity;
    public static boolean offline = false;
    public loadbalance(Activity activity){
        this.activity = activity;
    }

    public static String flatten(String filename){
        return filename.replaceAll("/",pathDeli);
    }

    public String toFullPath(String filename){
        return new File(activity.getFilesDir(),flatten(filename)).getAbsolutePath();
    }
    public String URLtoCache(String url){
        return toFullPath(url);
    }
    protected boolean cacheFile(String filename,String content){
        boolean success=false;
            try {

                FileOutputStream fos = activity.openFileOutput(flatten(filename), Context.MODE_PRIVATE);
                fos.write(content.getBytes());
                fos.close();
                success=true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        return success;
    }

    private String read(BufferedReader reader) throws IOException {
        char[] buffer = new char[1024*4];
        StringBuilder sb=new StringBuilder();
        int len=0;
        while ( ( len = reader.read(buffer)) >= 0){
            sb.append(buffer,0,len);
        }
        return sb.toString();

    }
    protected String readCache(String filename){
        String ret="";
        try {
            FileInputStream fis = activity.openFileInput(filename);
            InputStreamReader inputStreamReader = new InputStreamReader(fis);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            ret = read(bufferedReader);
        }catch (IOException e){
            e.printStackTrace();
        }
        return ret;
    }

    public String getContent(String url){
        String ret="";
        try {
            ret = getContent(new URL(url));
        }catch ( NullPointerException e){
            Log.d("What to do?","Null point exception, no connection");
        }
        catch (MalformedURLException e) {
            Log.d("Unsupported URL",url);
            e.printStackTrace();
        }
        return ret;
    }
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public String getContent(URL url) throws java.lang.NullPointerException{
        String content="";
        if ( url == null) return null;
        HttpURLConnection connection = null;
        boolean usecache = offline;
        File file = new File(URLtoCache(url.getPath()));
        String charset = "UTF-8";
        try {
            if ( offline ){
                usecache = file.exists();
            }else {
                connection = (HttpURLConnection) url.openConnection();
                if ( connection == null ) return null;
                String contentType = connection.getHeaderField("Content-Type");
                Long datetime = connection.getLastModified();

                Long fl = file.lastModified();
                usecache = file.exists() && (datetime > 0 && fl >= datetime);
                //usecache = false;

                if (contentType != null) {
                    for (String param : contentType.replace(" ", "").split(";")) {
                        if (param.startsWith("charset=")) {
                            charset = param.split("=", 2)[1];
                            if (charset.contains(",")){
                                charset="UTF-8";
                            }
                            break;
                        }
                    }
                }
            }
                    if (charset != null) {
                        InputStream is = null;
                        try {
                            try {
                                BufferedReader reader;
                                if ( usecache){
                                 reader = new BufferedReader(new FileReader(file));
                                }else {
                                    is = connection.getInputStream();
                                    reader = new BufferedReader(new InputStreamReader(is, charset));
                                }
                                content = read(reader);

                                if (is != null) {
                                    is.close();
                                }
                            } finally {
                                if (is != null) {
                                    is.close();
                                }
                            }
                        } catch (java.io.IOException e) {
                            e.printStackTrace();
                        }
                    }


            //Log.d("Content",String.format("%s:%s",url,content));
            if ( !usecache){
                FileOutputStream out = new FileOutputStream(file);
                out.write(content.getBytes("UTF-8"));
                out.close();
            }
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
        }catch(java.lang.NullPointerException e) {
            Log.d("Null","Connection cannot be made");
        }
        catch  (MalformedURLException e) {
            Log.d("Unkown URL",String.valueOf(e));
        }
        return ret;
    }
}
