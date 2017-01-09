package com.example.chenches.fullscreen;

import android.content.Intent;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by chenches on 8/17/2016.
 */
public class SSLTolerentWebViewClient extends WebViewClient {
    static final String referer = "Referer";
    private boolean addReferer = true;
    protected String lastURL = null;
    protected boolean oldversion = false;
    protected Map<String, String> defaultHeaders = new HashMap<>();
    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        handler.proceed(); // Ignore SSL certificate errors
    }

    protected Map<String,String> headerReWrite(String url, Map<String, String> headers){
        /*String ref=headers.get(referer);
        if ( ref == null || ref.endsWith("ad.html") ) { ref = (lastURL==null)?url:lastURL;}
        ref=ref.replaceAll("(^http.*?)(http(s)?:)","$2");
        if (headers.isEmpty()) {
            oldversion=true;
            //headers=defaultHeaders;
        }else{
            Log.d("Original Headers",String.format("%s -> %s",url,headers));
        }
        headers=defaultHeaders; // no matter what rewrite headers
        if ( addReferer) {
            headers.put(referer, ref);
        }*/
        headers=defaultHeaders; // no matter what rewrite headers
        headers.put("Accept-Language", "zh-CN,zh;q=0.8,en;q=0.6,ru;q=0.4");
        Log.d("New Headers",String.format("%s -> %s",url,headers));
        return headers;
    }

    private WebResourceResponse __sIR(WebView view, String u, final WebResourceRequest request) {
        URL url = null;
        Map<String, String> headers = new HashMap<>();
        final CountDownLatch haveHeaders = new CountDownLatch(1);
        final AtomicReference<Map<String, String>> headersRef = new AtomicReference<>();
        final CountDownLatch haveData = new CountDownLatch(1);
        final AtomicReference<InputStream> inputStreamRef = new AtomicReference<>();


        try {
            if (u != null) {
                url = new URL(u);
            } else {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    url = new URL(request.getUrl().toString());
                    headers = request.getRequestHeaders();
                }
            }
            if ( url != null ) {

                final URL finalUrl = url;
                final Map<String, String> finalHeaders = headerReWrite(url.toString(),headers);;
                new Thread() {
                    @Override
                    public void run() {
                        Log.d("To be Tracked", finalUrl.toString());
                        HttpURLConnection urlConnection = null;boolean redirect;int maxRedirect=5;
                        try {
                            urlConnection = (HttpURLConnection) finalUrl.openConnection();
                            Map<String, String> rh; int _redirect=0;
                            do {
                                redirect=false;
                                for (String key : finalHeaders.keySet()) {
                                    urlConnection.setRequestProperty(key, finalHeaders.get(key));
                                }
                                Log.d("Request:", String.format("%s->%s\n%s", finalUrl, finalHeaders, defaultHeaders));
                                    Map<String, List<String>> rawHeaders = urlConnection.getHeaderFields();
                                rh  = new HashMap<>();
                                if (rawHeaders != null) {
                                    for (Map.Entry<String, List<String>> entry : rawHeaders.entrySet()) {
                                        String headerName = entry.getKey();
                                        for (String value : entry.getValue()) {
                                            rh.put(headerName, value);
                                        }
                                    }
                                }
                                int status = urlConnection.getResponseCode();
                                if (status != HttpURLConnection.HTTP_OK) {
                                    if (status == HttpURLConnection.HTTP_MOVED_TEMP
                                            || status == HttpURLConnection.HTTP_MOVED_PERM
                                            || status == HttpURLConnection.HTTP_SEE_OTHER){
                                        URL redirectURL=new URL(urlConnection.getHeaderField("Location"));
                                        redirect=!finalUrl.equals(redirectURL) && ++_redirect < maxRedirect;
                                        if ( redirect ) urlConnection = (HttpURLConnection) redirectURL.openConnection();
                                    }
                                }
                            }while(redirect);
                            headersRef.set(rh);
                            // Copy headers from rawHeaders to headersRef
                            haveHeaders.countDown();

                            inputStreamRef.set(new BufferedInputStream(urlConnection.getInputStream()));
                            haveData.countDown();
                                    Log.d("Tracked", finalUrl.toString());
                            }catch(IOException e){
                                e.printStackTrace();
                            }

                    }
                }.start();
                return new WebResourceResponse(
                        null,
                        "UTF-8",
                        new InputStream() {
                            @Override
                            public int read() throws IOException {
                                try {
                                    haveData.await(100, TimeUnit.SECONDS);
                                    return inputStreamRef.get().read();
                                }catch(java.lang.NullPointerException n){
                                    Log.d("Didn't get inputstream","here");
                                    n.printStackTrace();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                return -1;
                            }}){

                                @Override
                                public Map<String, String> getResponseHeaders() {
                                    try {
                                        haveHeaders.await(100, TimeUnit.SECONDS);
                                        return headersRef.get();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    };
                                    return null;
                                }
                            };
             }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean needCustomHeader(URL url){
        /** any URL that return true, means we need to use headerrewrite rule
         *
         */
        return false;
    }

    public void tracking(String url){
        Log.d("Tracking",url);
    }

    private WebResourceResponse _sIR(WebView view, String u, WebResourceRequest request){
        URL url = null;
        Map<String, String> headers = new HashMap<>();

         try {
             if ( u != null ) {            url = new URL(u); }
             else {

                 if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                     if ( request.getUrl() != null ) {
                         url = new URL(request.getUrl().toString());
                         headers = request.getRequestHeaders();
                     }
                 }
             }
             if ( needCustomHeader(url)){
                 Log.d("CustomHeader",String.format("Trying to add referer %s",url.toString()));
                 return __sIR(view,u,request);
             }
             /*if ( url != null ){
                 if (! url.toString().contains("cansecs") && url.getPath().endsWith("get.json") ){
                     Log.d("get.json",String.format("Trying to add referer %s",url.toString()));
                     return __sIR(view,u,request);
                 }
             }*/
             tracking(url.toString());
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
    public boolean unknownURLHandler(WebView view,String url){
        try {
            Log.d("Unknown:", url);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            view.getContext().startActivity(intent);

            return true;
        } catch (android.content.ActivityNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }
    public boolean urlHandler(WebView view, String url,Map<String,String>headers){
        boolean handled=false;
        if ( url != null ) {
            if (!URLUtil.isNetworkUrl(url)) {
                Log.d("Non-standard URL",url);
                return unknownURLHandler(view,url);
            }
            headers = headerReWrite(url, headers);
            Log.d("URLHANDLING",url+":"+String.valueOf(headers));
            view.loadUrl(url, headers);
            handled = true;
        }
        return handled;
    }
    public boolean _sOUL(WebView view, String url,WebResourceRequest request){
        Map<String, String> headers = new HashMap<>();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP && request != null ) {
            headers = request.getRequestHeaders();
            if ( headers == null || headers.isEmpty()) headers = defaultHeaders;
            url=request.getUrl().toString();
        }
        if ( url.matches("^http(s)?.*$")) {
            lastURL = url;
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

    public void errorHandling(WebView view,int errorCode, String description, String failingUrl) {
        Log.d("View Error",String.format("%s on %s %d %s",view.getUrl(),failingUrl,errorCode,description));
    }
    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        errorHandling(view,errorCode,description,failingUrl);
    }

    @Override
    public void onReceivedError(WebView view, WebResourceRequest request,
                                WebResourceError error) {
        Log.d("Header", "Am I being called here?");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            String url = request.getUrl().toString(),desc="Unkown"; int errcode=-1;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                errcode=error.getErrorCode();
                desc=error.getDescription().toString();
            }
            errorHandling(view,errcode,desc,url);
        }
        //To Prevent  Web page not available

    }


}
