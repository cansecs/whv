package com.example.chenches.whp;

import android.content.Intent;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.util.Log;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by chenches on 8/17/2016.
 */
public class SSLTolerentWebViewClient extends WebViewClient {
    static final String referer = "Referer";
    public boolean addReferer = true;
    public String lastURL = null;
    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        handler.proceed(); // Ignore SSL certificate errors
    }
    public void headerReWrite(String url,Map<String, String>headers){
        String ref=headers.get(referer);boolean oldversion=false;
        if ( ref == null ) { ref = (lastURL==null)?url:lastURL;}
        ref=ref.replaceAll("(^http.*?)(http(s)?:)","$2");
        if (headers.isEmpty()) {
            if ( addReferer) {
                headers.put(referer, ref);
            }
            oldversion=true;
        }else{
            Log.d("Original Headers",String.format("%s -> %s",url,headers));
        }
        headers.put("Accept-Language", "zh-CN,zh;q=0.8,en;q=0.6,ru;q=0.4");
        Log.d("New Headers",String.format("%s -> %s",url,headers));
    }
    public WebResourceResponse _sIR(WebView view, String u, WebResourceRequest request){
        URL url = null;
        Map<String, String> headers = new HashMap<>();

         try {
             if ( u != null ) {            url = new URL(u); }
             else {

                 if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                     url = new URL(request.getUrl().toString());
                     headers = request.getRequestHeaders();
                 }
             }
             Log.d("Tracking",url.toString());

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }


        // intercept every call and modifying the header will be extemely slow :(
        /*
        try {
            URL url = null;
            Map<String, String> headers = new HashMap<>();

            if ( u != null ) url = new URL(u);
            else {
                Log.d("Header request","here");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    url = new URL(request.getUrl().toString());
                    headers = request.getRequestHeaders();
                }
            }
            Log.d("Header sir:",String.format("Url:%s -> header:%s",url,headers));
           if ( url == null) return null;
            headerReWrite(url.toString(),headers);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            for (String key : headers.keySet()) {
                connection.setRequestProperty(key, headers.get(key));
            }
            String type=connection.getContentType();
            String enc=connection.getContentEncoding();
            int code = connection.getResponseCode();
            Log.d("TYPEENC C",String.format("%s",code));
            if ( code != HttpURLConnection.HTTP_OK) {
                return null;
            }
            if ( enc == null ) {
                String deli="; ";
                int pos=type.indexOf(deli);
                if (pos > -1 ){
                    enc=type.substring(pos+deli.length()).replace("charset=","");
                    type=type.substring(0,pos);
                }
                if ( type == "text/html" && ( enc == null || enc == "")  ){
                    enc = "UTF-8";
                }
            }
            Log.d("TYPEENC",String.format("%s %s %s",url,type,enc));
            return new WebResourceResponse(type,enc, connection.getInputStream());

        } catch (java.net.SocketException e){
            Log.d("Header socket error:",String.format("%s",e));
        }catch(java.net.UnknownHostException e){
            Log.d("Header host error:",String.format("%s",e));
        }
        catch( MalformedURLException e) {
            Log.d("Header error:",String.format("%s",e));
        } catch (IOException e) {
            Log.d("IO Error:",String.format("%s",e));
        } catch (Exception e){
            Log.d("Unkonwn error:",String.format("%s",e));
        }*/
        return null;
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        WebResourceResponse wrr=null;
        wrr = _sIR(view,null,request);
        if ( wrr == null ) return super.shouldInterceptRequest(view,request);
        return wrr;
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP  ) {
            WebResourceResponse wrr = null;
            wrr = _sIR(view, url, null);
            if (wrr == null) return super.shouldInterceptRequest(view, url);
            return wrr;
        }else {
            return super.shouldInterceptRequest(view, url);
        }
    }
    public boolean urlHandler(WebView view, String url,Map<String,String>headers){
        boolean handled=false;
        if ( url != null ) {
            if (!URLUtil.isNetworkUrl(url)) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    view.getContext().startActivity(intent);
                    Log.d("Unknown:", url);
                    return true;
                } catch (android.content.ActivityNotFoundException e) {
                    e.printStackTrace();
                    return false;
                }
            }
            headerReWrite(url, headers);
            view.loadUrl(url, headers);
            handled = true;
        }
        return handled;
    }
    public boolean _sOUL(WebView view, String url,WebResourceRequest request){
        Map<String, String> headers = new HashMap<>();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP && request != null ) {
            headers = request.getRequestHeaders();
            url=request.getUrl().toString();
        }
        return urlHandler(view,url,headers);
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        return _sOUL(view,null,request);
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        return _sOUL(view,url,null);
    }


}
