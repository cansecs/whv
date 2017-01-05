package com.example.chenches.whp;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Environment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;
    private static final String TAG = "MAIN";
    private static final String UA = "WHP";
    private static boolean MENUSHOWN = false;
    private String HOMEPAGE = "";
    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    private WebView mWebView = null;
    private FrameLayout mTargetView = null;
    private FrameLayout mContentView = null;
    private WebChromeClient.CustomViewCallback mCustomViewCallback;
    private View mCustomView;

    private WebChromeClient mWebChromeClient = new WebChromeClient(){
        @Override
        public void onReceivedTitle(WebView view, String title){
            hide();
        }

        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            mCustomViewCallback = callback;
            mTargetView.addView(view);
            mCustomView = view;
            mContentView.setVisibility(View.GONE);
            mTargetView.setVisibility(View.VISIBLE);
            mTargetView.bringToFront();
            delayedHide(AUTO_HIDE_DELAY_MILLIS);
        }

        @Override
        public void onHideCustomView() {
            if (mCustomView == null)
                return;

            mCustomView.setVisibility(View.GONE);
            mTargetView.removeView(mCustomView);
            mCustomView = null;
            mTargetView.setVisibility(View.GONE);
            mCustomViewCallback.onCustomViewHidden();
            mContentView.setVisibility(View.VISIBLE);
        }


        @Override
        public void onProgressChanged(WebView view, int newProgress){
            Log.d("Progress",String.format("Progress:%d",newProgress));
        }

    };

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    //private View mContentView;
    @SuppressLint("InlineApi")
    private void fullscreen(View view){
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        hideSystemBar();
    }
    private void hideSystemBar(){
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
    }
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar
            fullscreen(mWebView);
            if (mCustomView !=null ) {
                fullscreen(mCustomView);
            }
        }
    };
    private GestureDetector mGestureDetector;
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    private boolean Download(String url) {
        String appName = getString(R.string.app_name).toLowerCase();
        String schema = appName+"://";
        int sl = schema.length();
        String filename="";
        String uRl="";
        if ( ! (url.substring(0,sl)).toLowerCase().equals(schema)) {
            return false;
        }else {
            url=url.substring(sl);
            int pos=url.indexOf("/");
            filename = url.substring(0,pos);
            uRl = url.substring(pos+1);
        }

        File direct = new File(Environment.getExternalStorageDirectory()
                + "/"+appName);

        if (!direct.exists()) {
            try {
                direct.mkdirs();}catch(java.lang.IllegalStateException e){
                Tips("无法缓存");
                return false;
            }
        }
        try {
            filename = URLDecoder.decode(filename,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        Log.d("Save:",String.format("%s -> %s",filename,uRl));
        DownloadManager mgr = (DownloadManager) this.getSystemService(Context.DOWNLOAD_SERVICE);

        Uri downloadUri = Uri.parse(uRl);
        DownloadManager.Request request = new DownloadManager.Request(
                downloadUri);
        String desc = filename;
        request.setAllowedNetworkTypes(
                DownloadManager.Request.NETWORK_WIFI
                        | DownloadManager.Request.NETWORK_MOBILE)
                .setAllowedOverRoaming(false).setTitle(desc)
                .setDescription(desc)
                .setDestinationInExternalPublicDir("/"+appName, filename);

        mgr.enqueue(request);
        Tips(String.format("缓存%s中",filename));
        return true;
    }
    private void Tips(String tips){
        loadJScript(String.format("_tipmsg.display('%s')",tips));
    }
    private void WebMenu(String action) {
        loadJScript(String.format("window.nomenuicon=true;window.myMenu && window.myMenu.%s", action));
    }

    private void showWebMenu() {
        hideCustomView();
        hide();
        WebMenu("Expand()");
        MENUSHOWN = true;
    }

    private void hideWebMenu() {
        WebMenu("Collapse()");
        MENUSHOWN = false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                break;
            case KeyEvent.KEYCODE_MENU:
                showWebMenu();
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void hideCustomView(){
        if (mCustomView != null) {
            mWebChromeClient.onHideCustomView();
        }
    }
    @Override
    public void onBackPressed(){
        if (mCustomView != null){
            hideCustomView();
        }else  if (MENUSHOWN) {
            hideWebMenu();
        } else if (mWebView.canGoBack()) {
            mWebView.goBack();
        }
       else{
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();    //To change body of overridden methods use File | Settings | File Templates.
        mWebView.onPause();
    }

    public void loadJScript(String script) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mWebView.evaluateJavascript(script, null);
        } else {
            mWebView.loadUrl(String.format("javscript:%s", script));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();    //To change body of overridden methods use File | Settings | File Templates.
        mWebView.onResume();
        delayedHide(AUTO_HIDE_DELAY_MILLIS);
    }

    @Override
    protected void onStop() {
        super.onStop();    //To change body of overridden methods use File | Settings | File Templates.
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Fullscreen Page", // TODO: Define a title for the content shown.
                // TODO: If you hafve web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.example.chenches.whp/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        // toggle();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.disconnect();
    }

    protected void trusteveryssl(){

        try {
            TrustManager[] victimizedManager = new TrustManager[]{

                    new X509TrustManager() {

                        public X509Certificate[] getAcceptedIssuers() {

                            X509Certificate[] myTrustedAnchors = new X509Certificate[0];

                            return myTrustedAnchors;
                        }

                        @Override
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
            };

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, victimizedManager, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String s, SSLSession sslSession) {
                    Log.d("Verify",s);
                    return true;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        trusteveryssl();
        setContentView(R.layout.activity_fullscreen);
        /*Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);*/

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);

        //mContentView = findViewById(R.id.fullscreen_content);


        // Set up the user interaction to manually show or hide the system UI.
        /*mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });*/

        mWebView = (WebView) findViewById(R.id.webView);
        mWebView.setWebViewClient(
                new SSLTolerentWebViewClient() {
                    /*@Override
                    public void onPageFinished(WebView view, String url) {
                        show();
                        MENUSHOWN = false;
                    }*/
                    @Override
                    public boolean urlHandler(WebView view, String url,Map<String,String>headers){
                        Log.d("URL Handling",url);
                        if (Download(url)){
                            Log.d("Downloading",url);
                            return true;
                        }else {
                            return super.urlHandler(view, url, headers);
                        }
                    }


                    private void displayErrorPage(WebView view) {
                        //view.loadData("", "", null);
                        String htmlData = getString(R.string.FailedPage);
                        view.loadUrl("about:blank");
                        view.loadDataWithBaseURL(null, htmlData, "text/html", "UTF-8", null);
                        view.invalidate();
                    }

                    @Override
                    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                        Log.d("Header", String.format("Am I being called here?:%s %s %s", errorCode, description, failingUrl));
                        displayErrorPage(view);
                    }

                    @Override
                    public void onReceivedError(WebView view, WebResourceRequest request,
                                                WebResourceError error) {
                        Log.d("Header", "Am I being called here?");
                        displayErrorPage(view);
                        //To Prevent  Web page not available

                    }
                }
        );
        // Here, we use #mWebChromeClient with implementation for handling PermissionRequests.
        mWebView.setWebChromeClient(mWebChromeClient);
        configureWebSettings(mWebView.getSettings());
        HOMEPAGE = getString(R.string.homeproto)+getString(R.string.homehost)+getString(R.string.homepage);
        mWebView.loadUrl(HOMEPAGE);

        Android_Gesture_Detector android_gesture_detector = new Android_Gesture_Detector() {
            @Override
            public void onLongPress(MotionEvent e) {

                //showWebMenu();

            }

            @Override
            public void onShowPress(MotionEvent e) {

                //showWebMenu();
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (e1.getY() < e2.getY()) {
                    Log.d("Gesture ", " Scroll Down");
                }
                if (e1.getY() > e2.getY()) {
                    // show();
                }
                return false;
            }
        };
// Create a GestureDetector
        mGestureDetector = new GestureDetector(this, android_gesture_detector);

        mWebView.setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View view, MotionEvent event) {
                mGestureDetector.onTouchEvent(event);
                return mWebView.onTouchEvent(event);
            }
        });
        mTargetView = (FrameLayout)findViewById(R.id.target_view);
        mContentView = (FrameLayout) findViewById(R.id.main_content);
        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        //findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }


    @SuppressLint("SetJavaScriptEnabled")
    private static void configureWebSettings(WebSettings settings) {
        settings.setJavaScriptEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        settings.setUseWideViewPort(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        //settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        String ua;
        ua = String.format("%s %s", settings.getUserAgentString(), UA);
        settings.setUserAgentString(ua);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        /*mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);*/

        mWebView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                |View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        setTitle(mWebView.getTitle());
        mVisible = true;
        if (AUTO_HIDE) {
            delayedHide(AUTO_HIDE_DELAY_MILLIS);
        }
        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);

    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Fullscreen Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.example.chenches.whp/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }
}
