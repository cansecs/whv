package com.example.chenches.fullscreen;

import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.os.StrictMode;
import android.provider.Settings;
import android.support.v4.view.WindowCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.RunnableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
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

    private static final String contentprovider = "content://whvcontent/";
    private static final String sLangCookie = "_v_";
    private static final String sLangPattern = String.format("^.*%s=([^;]+).*$",sLangCookie); // detect cookie lang pattern
    protected String sLANG = ""; // get what cookie value _v_ is, if it's zh_CN, it means chinese version, otherwise english
    private static final String sSchemaSuffix = "://";
    private static final String RELOAD = "reload"+sSchemaSuffix; // RELOAD schema
    private static String sAppName;
    protected static File sDownloaddir;
    private String sDownloadSchema; // webview page can call this schema to download and save url schema://filename/url
    private String sMenuSchema; // Schema on different menu action schema://action
    private static final boolean AUTO_HIDE = true;
    private static String UA;
    private static String sPlayerPage = "";
    protected DownloadManager Downloader;
    private localStreamer streamer = null;
    private int streamerPort = 11013;

    private String pHeaderPattern = "";
    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 5000;
    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private Menu oMenu;
    private static final int UI_ANIMATION_DELAY = 500;
    private static String originalUA;
    private static boolean MENUSHOWN = false;
    private final Handler mHideHandler = new Handler();
    DownloadMerger oDownloadMerger;

    protected static List<Object> innerObjects = new ArrayList<>();

    ArrayList<String> oHistories = new ArrayList<String>();

    class WHVHist{ // return once a page loaded, all the urls that webview has visited

        private String toJSONArr(List<String> list){
            return toJSONArr(list,true);
        }
        private String toJSONArr(List<String> list,boolean quote){
            String ret="[\"%s\"]",inside="",deli="\",\"";
            if (!quote){
                String quotes="\"";
                ret=ret.replaceAll(quotes,"");
                deli=deli.replaceAll(quotes,"");
            }

            int idx=0;
            for (String items : list) {
                inside+=items+(++idx<list.size()? deli:"");
            }
            return String.format(ret,inside);
        }
        @JavascriptInterface
        public void stopStreamer(){
            if (streamer != null ){
                if (streamer.isRunning()) streamer.stop();
            }
        }

        @JavascriptInterface
        public String getStreamerURL(){
            String ret="";
            if ( streamer != null){
             if (!streamer.isRunning()){
                 startStreamer();
             }
                ret=streamer.getUrl();
            }
            return ret;
        }

        /*@JavascriptInterface
        public void callmenu(String value){
            menuScript(value);
        }*/

        @JavascriptInterface
        public String visits() {
            return toJSONArr(oHistories);
        }

        @JavascriptInterface
        public String listDownloads() {
            return toJSONArr(listFiles(),false);
        }

        @JavascriptInterface
        public void removeFile(String filename){
            File file = new File(filename);
            Log.d("Removing",filename);
            file.delete();
        }
        private List<String> listFiles(){
            ArrayList<String> lists=new ArrayList<String>();
            String lu=this.getStreamerURL();
            if (oDownloadMerger != null){
               Map<String,Object> filemaps = oDownloadMerger.scanFolder();
                for (String name: filemaps.keySet()
                     ) {

                    File[] f=(File[]) filemaps.get(name);
                    ArrayList<String> items = new ArrayList<>();
                    items.add(name);
                    items.add("0");
                    long lm=0;
                    for (int i = 0; i < f.length; i++) {
                        File ff = f[i];
                        if (ff != null){
                            long _lm = ff.lastModified();
                            if (_lm > lm) lm=_lm;
                            items.add(ff.getAbsolutePath());
                        }
                    }
                    items.set(1,String.format("%s",lm));
                    lists.add(toJSONArr(items));
                }
            }
            return lists;
        }

        @JavascriptInterface
        public void _closeall(){
            close();
        }
        @JavascriptInterface
        public void Player(){
            loadPlayer();
        }
    }
    private static final String JSObj = "SNIFFER";
    private static final String JSPlayer = JSObj+".Player()";
    /**
     * Menu action string defined here
     */
    private static final String smHide = "Collapse()";
    private static final String smShow = "Expand()";
    private static final String smHome = "home";
    private static final String smBack = "arrow-l";
    private static final String smCallback = "callback";
    private static final String smMsg = "msg";
    private static final String smCache = "arrow-d";
    private static final String smForward = "arrow-r";
    private static final String smRefresh = "refresh";
    private static final String smStar = "star";
    private static final String smCloud = "cloud";
    private static final String smVisit = "navigation";
    private static final String smLanguage = "tag";
    private static final String smAccount = "account";
    private static final String smPower = "power";
    private static String defaultCookie = "";
    protected String androidId; // storing android UID to feed into cookie string to indentify the uniquness of a device
    private enum PAGESTATE { // repsents a page is in the following states
        PROVED, // URL host is in the sSites
        SAFE, // URL contains our subdomain
        UNKNOWN // undetected stage
    }
    private enum LANG {
        ENGLISH,
        CHINESE
    }
    private String currentSite = ""; // which site policy it should go
    private PAGESTATE oCurrentState = PAGESTATE.UNKNOWN;
    private LANG oCurrentLang = LANG.CHINESE;

    private Ads ads;
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
    private String HOMEPAGE;
    private String MENUPAGE;
    private SitePolicy oSitePolicy = new SitePolicy("{}"); // check if each has its own policy to deal customized UA, getVideoSrc any future functions
    protected WebView mWebView = null;
    protected WebView mMenuView = null;
    private FrameLayout mTargetView = null;
    private FrameLayout mContentView = null;
    private WebChromeClient.CustomViewCallback mCustomViewCallback;
    private View mCustomView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar
            fullscreen(true);
        }
    };
    private CookieManager mCookie;
    protected PointState pointState = new PointState(null);
    private String sSites = null; // Sites that APP can handle
    private String sJSSites = "";
    private String sLastURL = null; // Last URL app visited
    protected String sPrefix = null; // Site prefix we are supposed to visit
    private String sSafeSite = null;
    private GestureDetector mGestureDetector;
    //private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            fullscreen(false);
            //mControlsView.setVisibility(View.VISIBLE);
        }
    };
    //private ProgressDialog progressBar;
    private boolean mVisible;
    /*class JSI {
        private Context ctx;
        JSI(Context ctx){
            this.ctx=ctx;
        }
        public void showHTML(String html){
            Log.d("Script",html);
        }
    }*/
    private WebChromeClient mWebChromeClient = new WebChromeClient(){

        /*@Override
        public void onCloseWindow(WebView view){
            Log.d("Window close","Got called");
            loadJScript("window.HtmlViewer.showHTML"+
                    "(location.href+':'+document.getElementsByTagName('html')[0].outerHTML);"
            );
            if (view.canGoBack()) {
                (new menuActions()).BackorForward(true,"^.*?google.*$");
            }
        }*/
        @Override
        public void onReceivedTitle(WebView view, String title){
            show();
            feedMenu(view.getUrl());
        }

        private void setScreenOn(boolean value){
            int flag=WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
            if ( value ) {
                getWindow().addFlags(flag);
            }else{
                getWindow().clearFlags(flag);
            }
        }

        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            mCustomViewCallback = callback;
            mTargetView.addView(view);
            mCustomView = view;
            mContentView.setVisibility(View.GONE);
            mTargetView.setVisibility(View.VISIBLE);
            mTargetView.bringToFront();
            setScreenOn(true);
            hide();
        }

        @Override
        public void onHideCustomView() {
            if (mCustomView == null)
                return;
            //getWindow().addFlags(WindowManager.LayoutParams.FLAG);
            mCustomView.setVisibility(View.GONE);
            mTargetView.removeView(mCustomView);
            mCustomView = null;
            mTargetView.setVisibility(View.GONE);
            mCustomViewCallback.onCustomViewHidden();
            mContentView.setVisibility(View.VISIBLE);
            setScreenOn(false);
        }


        @Override
        public void onProgressChanged(WebView view, int newProgress){
          /*  //progressBar.show();
            progressBar.setMessage(String.format("%d%%...", newProgress));
            progressBar.setProgress(newProgress);*/
            if (newProgress == 100 ){
                //progressBar.hide();
                feedMenu(view.getUrl());
            }

        }

    };
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    private static void changeUA(WebSettings settings,String  newUA){
        if ( newUA == null || newUA.equals("")){
            newUA=originalUA;
        }
        settings.setUserAgentString(newUA);
    }
    private String UrltoPolDomain(String url){
        String p="";
        try {
            URL u = new URL(url);
            String host = u.getHost();
            p = host.replaceAll("^.*?(\\w+)\\.(\\w+)$","$1_$2");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return p;
    }
    private void feedMenu(String url){ // if url is not in safeSite, i.e. youtube for now, feed video src to menuView
        if ( url.toLowerCase().startsWith("http") && ! url.contains(sSafeSite) ){// unhandled by webpage
            //menuScript(String.format("pointstate={isvip:%s,isdemo:%s};",pointState.isVip(),pointState.isDemo()));
            _menuJSCheck(currentSite.isEmpty()?url:currentSite);
        }
    }
    private static void applyUA(WebSettings settings,String oUA){
        if ( oUA == null || oUA.isEmpty()){
            restoreUA(settings);
        }else {
            changeUA(settings, String.format("%s %s", oUA, UA));
        }
    }

    protected boolean isConnected(){
        NetworkInfo net = ((ConnectivityManager) getSystemService(Activity.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return net != null && net.isConnected();
    }

    private static void restoreUA(WebSettings settings){
        changeUA(settings,null);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebSettings(WebSettings settings) {
        settings.setJavaScriptEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            settings.setAllowFileAccess(true);
            settings.setMediaPlaybackRequiresUserGesture(false);
            //settings.setAllowFileAccessFromFileURLs(true);
            //settings.setAllowUniversalAccessFromFileURLs(true);
        }


        settings.setSupportMultipleWindows(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(false);
        settings.setUseWideViewPort(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);

        int mode = WebSettings.LOAD_DEFAULT;
        if (!isConnected()){
            mode=WebSettings.LOAD_CACHE_ELSE_NETWORK;
        }
        settings.setCacheMode(mode);
        settings.setAppCacheEnabled(true);
        settings.setAppCachePath(getCacheDir().getPath());

        //settings.setAllowFileAccess(true);

        if ( originalUA == null ) {
            originalUA=String.format("%s %s", settings.getUserAgentString(), UA);
        }
        changeUA(settings,null);
    }


    private void fullscreen(boolean enter){
        View [] views = { null,
                //mContentView
                //,mWebView
                //, mMenuView
                getWindow().getDecorView()
        };
        for (int i = 0; i < views.length; i++) {
            if ( views[i] != null ){
                hideSystemBar(enter,views[i]);
            }
        }
    }
    private void hideSystemBar(boolean isRotated,View view){
        if ( view == null ){
            view = getWindow().getDecorView();
        }
        if ( ! view.isShown() ){
            return;
        }
        int value = view.getSystemUiVisibility();

        //mContentView.setFitsSystemWindows(false);
        android.app.ActionBar ab = getActionBar();
        ActionBar ab2 = getSupportActionBar();

        if ( isRotated) {
            value=
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_FULLSCREEN
            ;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                value = value
                     //   | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_IMMERSIVE
                ;
            }
            //getActionBar().hide();
        }else{
            value &= ~View.SYSTEM_UI_FLAG_FULLSCREEN
                    //|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            ;
            //getActionBar().show();
           /* if ( ab2 != null && !ab2.isShowing()){
                ab2.show();
            }else {
                if (ab != null && !ab.isShowing()) {
                    ab.show();
                }
            }
            */
        }
        //Log.d("Setting view",String.format("%s",value));
        //view.getRootView().setFitsSystemWindows(!isRotated);
        int topp=0;
        if (!isRotated){
            topp=paddingDp;
        }
        View otherview = mWebView;
        if (mMenuView.isShown()){
            otherview = mMenuView;
        }
        otherview.setPadding(0,topp,0,topp);
        view.setSystemUiVisibility(value);
        //view.invalidate();

    }

    private boolean isEnglish(){
        return oCurrentLang == LANG.ENGLISH;
    }

    protected String T(String english,String chinese){
        return isEnglish()?english:chinese;
    }
    private void displayErrorPage(WebView view,String msg) {
        //view.loadData("", "", null);
        String htmlData = getString(R.string.FailedPage);
        String title=T("Error","出了点问题");
        String common = T("Network issue?","网络有问题？")+"<br/>";
        if (msg == null || msg.isEmpty()){
            msg= String.format(T("Take a break, then click <a href='%s'>here to retry.</a>, or check your <a href='javascript:%s'>cached</a> files?","休息一会儿点<a href='%s'>这里</a>再试一下,或者看看<a href='javascript:%s'>缓存的视频</a>"),RELOAD,JSPlayer);
        }
        view.loadUrl("about:blank");
        //view.loadData(String.format(htmlData,title,msg+common), "text/html", "UTF-8");
        view.loadDataWithBaseURL(HOMEPAGE, String.format(htmlData,title,common+msg), "text/html", "UTF-8", null);
        //view.invalidate();
    }

    private boolean Download(String url) {
        String appName = sAppName;
        String schema = sDownloadSchema;
        String cannot = T("Sorry cannot cache!","对不起，无法缓存！");

        int sl = schema.length();
        String filename;
        String uRl;
        if ( ! (url.substring(0,sl)).toLowerCase().equals(schema)) {
            return false;
        }else {
            url=url.substring(sl);
            int pos=url.indexOf("/");
            if ( pos == -1) return false;
            filename = url.substring(0,pos).trim();
            uRl = url.substring(pos+1);
            try {
                filename = URLDecoder.decode(filename,"UTF-8");
                uRl = URLDecoder.decode(uRl,"UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }



            try{
                new URL(uRl);
            }catch(MalformedURLException e){
                Tips(cannot);
                Log.d("Bad url format",String.format("%s -> %s",uRl,e));
                return false;
            }
        }

        File direct = sDownloaddir, save = new File(sDownloaddir,filename);

        if (!direct.exists()) {
            boolean success=false;
            try {
                success=direct.mkdirs();
            }catch(java.lang.IllegalStateException e){
                Tips(cannot);
                success=false;
            }
            if (!success) return !success;
        }
        Log.d("Save:",String.format("%s -> %s",filename,uRl));
        DownloadManager mgr = Downloader;

        Uri downloadUri = Uri.parse(uRl),saveUri = Uri.parse(save.toURI().toString());
        DownloadManager.Request request = new DownloadManager.Request(
                downloadUri);
        String cookie = mCookie.getCookie(uRl);
        String ua = mWebView.getSettings().getUserAgentString();
        request.addRequestHeader("Cookie",cookie);
        request.addRequestHeader("User-agent",ua);
        String desc = filename;
        request.setAllowedNetworkTypes(
                DownloadManager.Request.NETWORK_WIFI
                        | DownloadManager.Request.NETWORK_MOBILE)
                .setAllowedOverRoaming(false).setTitle(desc)
                .setVisibleInDownloadsUi(true)
                .setDescription(desc)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationUri(saveUri);
                //.setDestinationInExternalPublicDir("/"+appName, filename);

        mgr.enqueue(request);
        Tips(String.format(T("Caching %s","缓存%s中"),filename));
        return true;
    }

    /*
    private String menuCallback(String callback){
        return String.format("%s%s/%s",sMenuSchema,smCallback,callback);
    }
    private String menuPrompt(String msg){
        return String.format("%s%s/%s",sMenuSchema,smMsg,msg);
    }
*/
    private void _menuJSCheck(String url) { // keep checking unmanaged website and transfer video src and url and title to menu for posting video, adding favorite and history
        /*loadJScript(",t='"+mWebView.getTitle()+"',cb='videoURL=\""+mWebView.getUrl()+"\";videoTitle=\"'+t+'\";',c=function(s){document.location=s};" +
                "cb+=v?'$_video=_fv(\"'+v.src+'\");_pv(1);$_vl='+JSON.stringify({l:v.src,title:t})+';':'';"+String.format("c('%s')",menuCallback("'+cb+'")));
        */
        String ps=oSitePolicy.getScript(url),script=pointState.isVip()?"isvip=1;":"";
        if ( ps != null) {
            script+=ps;
        }else{
            script+="(function(cb){cb('VideoURL=\"'+location.href+'\"')})";
            /*
                    ",t='"+mWebView.getTitle()+"',cb='videoURL=\""+mWebView.getUrl()+"\";videoTitle=\"'+t+'\";',c=function(s){document.location=s};" +
                    "cb+=v?'$_video=_fv(\"'+v.src+'\");_pv(1);$_vl='+JSON.stringify({l:v.src,title:t})+';':'';"+String.format("c('%s')",menuCallback("'+cb+'")));
            */
        }
        if ( !script.isEmpty()) loadJScript(script+String.format("(function(v){ var url='%s%s/'+v,doc=document,a=doc.createElement(\"a\");\n" +
                "  a.href=url;\n" +
                "  doc.body.appendChild(a);a.click();doc.body.removeChild(a)\n" +
                "  delete a;" +
                "})",sMenuSchema,smCallback));
        /*loadJScript("var _m=function(){var v=document.querySelector('video'),sd=false,c=JSON.stringify,_n={l:v?v.src:'',title:document.title,u:location.href};if(!window._s){_s=_n;sd=true}" +
                "if(c(_n)!=c(_s)){_s=_n;sd=true};"+
                "var cb='videoURL=\"'+_s.u+'\";videoTitle=\"'+_s.title+'\";'"+
                "+(_s.l?('$_video=_fv(\"'+_s.l+'\");_pv(1);$_vl='+JSON.stringify(_s)):'');"+
                String.format("if(sd){document.location='%s'}};_t=setInterval(_m,1e3);",menuCallback("'+cb+'")));
                */
    }

    private void Tips(String tips){
        /*String script = String.format("window._tipmsg && _tipmsg.display('%s')", tips);
        if ( oCurrentState.equals(PAGESTATE.SAFE)) {
            loadJScript(script);
        }else{
            showWebMenu();
            menuScript(script);
        }*/
        Toast toast = Toast.makeText(this,tips,Toast.LENGTH_LONG);
        toast.setGravity(Gravity.BOTTOM| Gravity.LEFT, 0, 0);
        toast.show();
    }

    private void WebMenu(String action) {
        String workaround=String.format("window.location='%s%s'",sMenuSchema,action); // if there is out of safesize, i.e. in youtube, launch menu by using specific url
        if ( oCurrentState != PAGESTATE.PROVED) {
            loadJScript(String.format("window.nomenuicon=true;if(window.myMenu){window.myMenu.%s}else{if(!window.myapp)%s}", action,workaround));
        }else {
            loadJScript(workaround);
        }

    }

    protected void showWebMenu() {
        hideCustomView();
        hide();
        WebMenu(smShow);
        MENUSHOWN = true;
    }

    protected void hideWebMenu() {
        WebMenu(smHide);
        MENUSHOWN = false;
    }

    protected void toggleWebMenu(){
        if (MENUSHOWN){
            hideWebMenu();
        }else{
            showWebMenu();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        hide();
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                break;
            case KeyEvent.KEYCODE_MENU:
                toggleWebMenu();
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
            (new menuActions()).back();
        }
        else{
            finish();
        }
    }

    @Override
    protected void onRestoreInstanceState (Bundle savedInstanceState){
        callAll("onRestoreInstanceState",savedInstanceState);
        super.onRestoreInstanceState(savedInstanceState);
    }

    protected void call(List<Object> objects, String method, Bundle... args){
        Method methodobj = null;
        String name="Unkownn";
        for (Object obj:objects
             ) {
            try {
                Class oc=obj.getClass();
                name = oc.getName();
                methodobj = oc.getMethod(method);
            } catch (NoSuchMethodException|SecurityException e) {
                Log.d("Unsupported Methd",String.format("Object:%s doesn't have method:%s, ignoring",name,method));
            }
            try{
                if ( methodobj != null ) {
                    if ( args.length > 0) {
                        Bundle[] bs = Arrays.copyOfRange(args, 0, args.length);
                        methodobj.invoke(obj,(Object)bs);
                    }else{
                        methodobj.invoke(obj);
                    }
                }
            }catch(IllegalArgumentException|IllegalAccessException|InvocationTargetException e){
               Log.d("Illegal call",name);
            }

        }
    }

    protected void callAll(String method,Bundle... args){
        call(innerObjects,method,args);
    }
    @Override
    protected void onPause() {
        super.onPause();    //To change body of overridden methods use File | Settings | File Templates.
        callAll("onPause");
    }

    private void _loadScript(String script,WebView view){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            view.evaluateJavascript(script, null);
        } else {
            view.loadUrl(String.format("javscript:%s", script));
        }

    }
    protected void loadMenuPage(){
        String url = mWebView.getUrl();
        if ( url == null ) return;
        String suburl=url.replaceAll("^(https://.*?)(http(?:s)?://.*)$","$2");
        if (!(currentSite == null || currentSite.isEmpty())) suburl=currentSite;
        String site = oSitePolicy.getKey(suburl,true);
        loadMenuPage(site);

    }
    protected void loadMenuPage(String dm){
        String mp=MENUPAGE;
        if ( dm != null && !dm.isEmpty()){
            mp=mp.replaceAll("(//)www(\\.)","$1"+dm+"$2");
        }
        if ( !mp.equalsIgnoreCase(mMenuView.getUrl())){
            mMenuView.loadUrl(mp);}
        else{
            menuScript("$_B.check();");
        }
    }
    public void loadJScript(String script) {
        _loadScript(script,mWebView);
    }

    public void menuScript(String script){
        _loadScript(script,mMenuView);
    }

    @Override
    protected void onResume() {
        super.onResume();    //To change body of overridden methods use File | Settings | File Templates.
        //mWebView.onResume();
        //mMenuView.onResume();
        callAll("onResume");
        hide();
    }

    @Override
    protected void onStop() {
        super.onStop();    //To change body of overridden methods use File | Settings | File Templates.
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Fullscreen Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.example.chenches.fullscreen/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        toggle();
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
    private class menuActions{
        public boolean show(){
            if ( mMenuView.getVisibility() == WebView.INVISIBLE){
                mWebView.setVisibility(WebView.INVISIBLE);
                mMenuView.setVisibility(WebView.VISIBLE);
            }
            return mMenuView.getVisibility() == WebView.VISIBLE;
        }
        public boolean hide(){
            if ( mMenuView.getVisibility() == WebView.VISIBLE){
                loadMenuPage();
                mWebView.setVisibility(WebView.VISIBLE);
                mMenuView.setVisibility(WebView.INVISIBLE);
            }
            return mMenuView.getVisibility() == WebView.INVISIBLE;
        }
        public void visit(){callback(String.format("_visit('',function(v){ location.href='%s/'+v})",sMenuSchema+smHome));}
        public void language(){ String s="window._LANG && _LANG.switch_version();"; loadJScript(s); callback(s);}
        public void back(){
            BackorForward(true);
        }
        public void forward(){
            BackorForward(false);
        }
        public void message(String script){
            String c="/";
            String[] items = script.split(c);
            if ( items.length < 2) {
                return;
            }
            String args=String.format("'%s','%s'",items[0],items[1]);
            for ( int i=2;i<items.length;i++){
                args+=((i==2)?",":c)+items[i];
            }
            String s=String.format("jPrompt(%s)",args);
            Log.d("Message:","From "+script+" To "+s);
            show();
            menuScript(s);
        }
        public void account(){
            pickUserAccount();
        }
        public void refresh(){
            mWebView.reload();
        }
        public void star(){
            //String url=mWebView.getUrl();
            menuScript("_bm('',window.videoTitle);");
        }
        public void callback(String script){
            menuScript(script);
        }
        public void cache(){
            menuScript("_dd();");
        }
        public void home(String url){
            if ( url == null || url.isEmpty() ) url=HOMEPAGE;
            String targetUrl=mWebView.getUrl();
            menuScript(String.format("targetURL='%s'",targetUrl));
            if ( !url.equalsIgnoreCase(targetUrl))
                mWebView.loadUrl(url);
        }
        public void power(){
            menuScript("$_B.signoff=true;");
        }
        public void cloud(){
            menuScript("_pv();");
        }
        public void BackorForward(boolean d,String until){
            WebBackForwardList wbfl  = mWebView.copyBackForwardList();
            int s=(d)?-1:1,i=wbfl.getCurrentIndex()+s,j=s;
            while (i > -1 && i < wbfl.getSize()){
                String url=wbfl.getItemAtIndex(i).getUrl();
                Log.d("History:",url+String.valueOf(i));
                if ( ! url.matches(until)){
                    //if ( !url.toLowerCase().startsWith("javascript:") ){
                    break;
                }
                j+=s;
                i+=s;
            }
            if ( mWebView.canGoBackOrForward(j)) mWebView.goBackOrForward(j);
        }
        private void BackorForward(boolean d){
            BackorForward(d,"^javascript:.*$");
        }
    }

    class menuJSObject {

        @JavascriptInterface
        public boolean click(String url){
            return menuHandler(url);
        }
    }

    private boolean menuHandler(String url){
        boolean handled = false,keep=false;
        menuActions m = new menuActions();
        if ( url.toLowerCase().startsWith(sMenuSchema)){
            try {
                url=URLDecoder.decode(url,"UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            String action=url.substring(sMenuSchema.length());
            int pos = action.indexOf('/');String rest="";
            if ( pos > -1 ){
                rest=action.substring(pos+1);
                action=action.substring(0,pos);
            }
            Log.d("MenuAction",url+"--"+action+":"+rest);
            switch(action){
                case smShow: {
                    keep=m.show();
                    break;
                }
                case smVisit:{
                    m.visit();
                    keep=true;
                    break;
                }
                case smAccount:{
                    m.account();
                    break;
                }
                case smPower:{
                    m.power();
                    break;
                }
                case smLanguage:{
                    m.language();
                    break;
                }
                case smHide:{
                    m.hide();
                    break;
                }
                case smCloud:{
                    m.cloud();
                    keep=true;
                    break;
                }
                case smCallback: {
                    m.callback(rest);
                    break;
                }
                case smMsg:{
                    m.message(rest);
                    keep=true;
                    break;
                }
                case smHome:{
                    m.home(rest);
                    break;
                }
                case smBack:{
                    m.back();
                    break;
                }
                case smStar:{
                    m.star();
                    keep=true;
                    break;
                }
                case smCache:{
                    m.cache();
                    keep=true;
                    break;
                }
                case smForward:{
                    m.forward();
                    break;
                }
                case smRefresh:{
                    m.refresh();
                    break;
                }
                default: break;

            }
            if ( !keep) m.hide();
            handled=true;
        }
        return handled;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.home:
                toggleWebMenu();
                return true;
            case R.id.points:
                // TODO Add in-App Purchase function here
                //appnext.run();
                ads.run();
                return true;
            case R.id.menu:
                loadPlayer();
                return true;
            case R.id.close:
                close();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu resource
        oMenu = menu;
        getMenuInflater().inflate(R.menu.menu, menu);

        // Retrieve the share menu item
        //MenuItem shareItem = menu.findItem(R.id.menu_share);

        // Now get the ShareActionProvider from the item
        /*mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(shareItem);

        // Get the ViewPager's current item position and set its ShareIntent.
        int currentViewPagerItem = ((ViewPager) findViewById(R.id.viewpager)).getCurrentItem();
        setShareIntent(currentViewPagerItem);
        */
        return super.onCreateOptionsMenu(menu);
    }

    static int REQUEST_CODE_PICK_ACCOUNT = 1002;
    public void pickUserAccount() {
    /*This will list all available accounts on device without any filtering*/
        Intent intent = AccountPicker.newChooseAccountIntent(null, null,
                null, true, T("Please choose an acount to login","请选择一个邮件帐户登录"), null, null, null);
        startActivityForResult(intent, REQUEST_CODE_PICK_ACCOUNT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICK_ACCOUNT) {
            // Receiving a result from the AccountPicker
            if (resultCode == RESULT_OK) {
                //System.out.println(data.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE));
                String email = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME),from="APP",fullname="TODO";
                loadJScript(String.format("window.redirect && redirect('%s','%s','%s')",email,fullname,from));
            } else if (resultCode == RESULT_CANCELED) {
                //Toast.makeText(this, R.string.pick_account, Toast.LENGTH_LONG).show();
                pickUserAccount();
            }
        }
    }
    @Override
    public boolean onSupportNavigateUp(){
        toggleWebMenu();
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle state){
        super.onSaveInstanceState(state);
        callAll("onSaveInstanceState",state);
    }

    int paddingDp = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().requestFeature(WindowCompat.FEATURE_ACTION_BAR_OVERLAY);
        super.onCreate(savedInstanceState);
        callAll("onCreate",savedInstanceState);
        int paddingPixel = 25;
        float density = getResources().getDisplayMetrics().density;
        paddingDp = (int)(paddingPixel * density);
        final View view =
        getWindow().getDecorView();
        view.setFitsSystemWindows(false);
        view.setOnSystemUiVisibilityChangeListener(
                (new View.OnSystemUiVisibilityChangeListener(){
                  @Override
                    public void onSystemUiVisibilityChange(int visibility){
                     // fullscreen(false);
                      //getWindow().getDecorView().setPadding(0,paddingDp,0,0);
                  }
                })
        );
        fullscreen(rotated());
        Downloader = (DownloadManager) this.getSystemService(Context.DOWNLOAD_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        } // enable webview debug?
        //Typeface.createFromAsset(getAssets(), "fontawesome-webfont.ttf");
        UA = getString(R.string.app_name);
        // appnext = new Appnext(this);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        trusteveryssl();
        ActionBar actionBar = getSupportActionBar();
        try {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setHomeAsUpIndicator(android.R.drawable.ic_dialog_dialer);
        }catch(java.lang.NullPointerException e){
            Log.d("Cannot setup","Display Home icon");
        }
        //R.drawable.menu_small

        //actionBar.setHomeAsUpIndicator();
        setContentView(R.layout.activity_fullscreen);


        /*Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);*/
        sAppName = getString(R.string.app_name).toLowerCase();

        sDownloaddir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),getString(R.string.app_desc));
        streamer = new localStreamer(sDownloaddir.getAbsolutePath()); // initiate a stream to handle remote call to get local content
                //Environment.getExternalStorageDirectory().toString()+"/"+sAppName;

        oDownloadMerger =  new DownloadMerger(this);


        sDownloadSchema = sAppName + sSchemaSuffix;
        sMenuSchema = "menu"+sDownloadSchema;

        mVisible = true;
        /*mControlsView = findViewById(R.id.fullscreen_content_controls);
        progressBar = new ProgressDialog(mControlsView.getContext());
        //progressBar.getWindow().setGravity(Gravity.BOTTOM);
        progressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressBar.setProgress(0);
        progressBar.setMax(100);*/
        //mContentView = findViewById(R.id.fullscreen_content);

        // Set up the user interaction to manually show or hide the system UI.
        /*mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });*/
        mMenuView = (WebView) findViewById(R.id.menuView);
        mMenuView.setWebViewClient(new SSLTolerentWebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                menuScript(sJSSites);
            }

            @Override
            public boolean urlHandler(WebView view, String url, Map<String, String> headers) {
                if (url.toLowerCase().startsWith(sDownloadSchema)){
                    Download(url);
                    Log.d("Downloading",url);
                    return true;
                }else  if (url.toLowerCase().startsWith(sMenuSchema)) {
                    return menuHandler(url);
                }else if (!url.toLowerCase().contains(sSafeSite) ) {
                    return unknownURLHandler(view,url);
                }
                return super.urlHandler(view, url, headers);
            }

            @Override
            public void errorHandling(WebView view,int errorCode, String description, String failingUrl) {
                super.errorHandling(view,errorCode,description,failingUrl);
                if ( ! view.getUrl().startsWith("data:")) {
                    displayErrorPage(view, String.format("<a href='%s'>", JSObj + "._closeall()") + T("Please close this APP completedly, check your netork and reopen it again!", "请关闭APP，检查网络，确定网络正常重新启动这个APP试试看") + "</a>");
                }
            }
        });

        mWebView = (WebView) findViewById(R.id.webView);
        /*disabling blob: url

         */

        class dumb{
            @JavascriptInterface
            void nothing(){
            }
        }

        //mWebView.addJavascriptInterface(new dumb(),"MediaSource");
        //mWebView.addJavascriptInterface(new dumb(),"webkitMediaSource");

        mWebView.addJavascriptInterface(new WHVHist(),JSObj);

        mWebView.setWebViewClient(
                new SSLTolerentWebViewClient() {
                    boolean needToSetCookie = true;
                    void setCookie(CookieManager cm,String url,String key,String value){
                        if ( key.isEmpty()) return;
                        String pair=(key.contains("="))?key:String.format("%s=%s",key,value);
                        cm.setCookie(url,String.format("%s;path=/;domain=%s",pair,getString(R.string.subdomain)));
                    }
                    @Override
                    public void tracking(String url){
                        oHistories.add(url);
                    }
                    @Override
                    public void onPageStarted(WebView view,String url,Bitmap favicon){
                        oHistories.clear();
                        if (!needToSetCookie) return;
                        needToSetCookie=false;
                        mCookie = CookieManager.getInstance();
                        mCookie.setAcceptCookie(true);
                        //String defaultCookie=getString(R.string.defaultCookie);
                        String[] items = defaultCookie.split(";");
                        for ( int i=0;i<items.length;i++){
                            String[] kv=items[i].split("=");
                            String cookie=mCookie.getCookie(url);
                            if (cookie == null || !cookie.matches(".*?"+kv[0]+"=.*?"))
                                setCookie(mCookie,url,items[i].trim(),"");
                        }

                    }
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        mCookie = CookieManager.getInstance();
                        mCookie.setAcceptCookie(true);
                        //String cok=String.format("%s=%s;path=/;domain=%s",getString(R.string.uuid),androidId,getString(R.string.subdomain));
                        //mCookie.setCookie(url,cok);
                        if (androidId != null ) setCookie(mCookie,url,getString(R.string.uuid),androidId);
                        String cookie=mCookie.getCookie(url);
                        if ( cookie != null ) {
                            if (cookie.matches(sLangPattern)) {
                                sLANG = cookie.replaceAll(sLangPattern, "$1");
                                if ( sLANG.equals("zh-CN")){
                                    oCurrentLang = LANG.CHINESE;
                                }else{
                                    oCurrentLang = LANG.ENGLISH;
                                }
                            }else{
                                setCookie(mCookie,url,sLangCookie,T("en-US","zh-CN"));
                            }
                            pointState.parse(cookie);
                            Log.d("Point",pointState.toString());
                            oMenu.findItem(R.id.points).setTitle(T("Points","积分")+"\n"+pointState.getPoints());
                            oMenu.findItem(R.id.menu).setTitle(T("Local cache","本地缓存"));
                            oMenu.findItem(R.id.close).setTitle(T("Exit","退出"));
                        }
                        //if ( oldversion ){
                        String othercookie="",referer=lastURL;
                        if ( referer != null && ! referer.isEmpty()) {
                            referer = referer.replaceAll("(^http.*?)(http(s)?:)", "$2");
                            if (!referer.equalsIgnoreCase(lastURL)) {
                                othercookie = mCookie.getCookie(referer);
                            }
                            if ( referer.matches("^.*youku.*$")) {
                                referer = "http://v.youku.com"; // youku get.json doesn't like the referer to be just the page itself
                                // TODO make this into policy?
                            }
                        }
                        defaultHeaders.put("Other",othercookie);
                        defaultHeaders.put("User-Agent",originalUA);
                        defaultHeaders.put("Cookie",cookie);
                        defaultHeaders.put("Referer",referer);
                        //}
                        feedMenu(view.getUrl());
                    }

                    @Override
                    protected Map<String,String> headerReWrite(String url, Map<String, String> headers){
                        for (String key : defaultHeaders.keySet()) {
                            headers.put(key, defaultHeaders.get(key));
                        }
                        if (!url.toLowerCase().contains(sSafeSite) ) {
                            String cookie= headers.get("Other");
                            headers.put("Cookie", cookie);
                            String referer = headers.get("Referer");
                            if ( referer != null && referer.contains(sSafeSite)){
                                headers.put("Referer",url);
                            }
                        }
                        headers.remove("Other");
                        return headers;
                    }

                    @Override
                    public boolean needCustomHeader(URL url){
                        boolean intercept=false;
                        if ( url != null ) {
                            String host = url.getHost(), path = url.getPath();
                            intercept = ( ! host.contains(getString(R.string.subdomain))) && (
                                    (pHeaderPattern.isEmpty() || url.toString().matches(pHeaderPattern) )
                                            /*||
                                    (( url.getProtocol().compareToIgnoreCase("http") == 0 && host.compareToIgnoreCase("ubercpm.com") == 0) && path.endsWith("show.php"))
                                            || path.endsWith("get.json")
                                            || path.endsWith("api/modules/api") */
                            );


                        }
                        return intercept || super.needCustomHeader(url);
                    }
                    @Override
                    public void onLoadResource(WebView view,String url){
                        // Log.d("URL Handling onload:",url);
                    }
                    @Override
                    public boolean urlHandler(WebView view, String url,Map<String,String>headers){
                        WebSettings settings = view.getSettings();
                        String origUrl = url;
                        Log.d("URL Handling",url);
                        oCurrentState = PAGESTATE.UNKNOWN;
                        if (url.toLowerCase().startsWith(sDownloadSchema)){
                            Download(url);
                            Log.d("Downloading",url);
                            return true;
                        }else if (url.startsWith(sMenuSchema)){
                            return menuHandler(url);
                        }else if ( url.startsWith(contentprovider)){
                            Log.d("Contenthandler",url);
                            url=url.replace(contentprovider,sPrefix+"/");
                            return true;
                        }else if(url.startsWith("about:")){
                            return false;
                        }
                        else if ( url.startsWith(RELOAD)) { // server error, try to relocate the server URL
                            Launch();
                            return true;
                        }
                            //String script = oSitePolicy.getScript(url);
                        String suburl=url.replaceAll("^(https://.*?)(http(?:s)?://.*)$","$2"),
                                site = oSitePolicy.getKey(suburl,true),
                                ref=defaultHeaders.get("Referer"),
                                refscript = oSitePolicy.getScript(ref),
                                lastsite = oSitePolicy.getKey(ref,true),
                                script = (refscript !=null && !refscript.isEmpty())?refscript:oSitePolicy.getScript(suburl);

                        if (url.toLowerCase().contains(sSafeSite) ) {
                            oCurrentState = PAGESTATE.SAFE;
                            if (! script.isEmpty() &&  !suburl.equalsIgnoreCase(url)){
                                url=suburl;
                                oCurrentState = PAGESTATE.PROVED;
                            }
                            lastsite=null;
                        }
                        currentSite = (lastsite==null || lastsite.isEmpty())?suburl:ref;
                        loadMenuPage();
                        if ( oCurrentState == PAGESTATE.UNKNOWN ){
                            Log.d("Unhandled:", url);
                            try {
                                URL u = new URL(url);
                                String host = u.getHost();
                                boolean matched = host.toLowerCase().matches(sSites) || !site.isEmpty();
                                Log.d("Unhandled", String.format("%s->%s->%s", host, sSites, matched));
                                if ( !script.isEmpty() || ( !sLastURL.isEmpty() && (origUrl.contains(sLastURL) || sLastURL.contains(origUrl)))) {
                                    Log.d("Unhandled loop", "true:"+origUrl+":"+sLastURL);
                                    sLastURL = origUrl;
                                    if ( matched){
                                        oCurrentState = PAGESTATE.PROVED;
                                    }
                                    return false;
                                }else if (matched ) {
                                    url = sPrefix + "/cn1p3/" + url;
                                    Log.d("Bring it back", url);
                                    oCurrentState = PAGESTATE.PROVED;
                                }
                            } catch (MalformedURLException e) {
                                e.printStackTrace();
                            }
                            Log.d("Unhandled original", String.format("%s %s %s", origUrl, url, sLastURL));
                        }

                        if ( oCurrentState == PAGESTATE.UNKNOWN ) {
                            return unknownURLHandler(view, url);
                        }
                        sLastURL = url;
                        String ua=oSitePolicy.getUA(url);
                        applyUA(settings, ua);   // change UA to be able to send video URL, if it's mobile, it's using BLOB: URL
                        return super.urlHandler(view, url, headers);
                    }

                    @Override
                    public void errorHandling(WebView view,int errorCode, String description, String failingUrl) {
                        super.errorHandling(view,errorCode,description,failingUrl);
                        if (failingUrl.equalsIgnoreCase(view.getUrl())){ // TODO: 10/3/2016 check the errorCode?
                            displayErrorPage(view,"");
                        }
                        // ignore it
                    }

                }
        );
        // Here, we use #mWebChromeClient with implementation for handling PermissionRequests.
        mWebView.setWebChromeClient(mWebChromeClient);
        Android_Gesture_Detector android_gesture_detector = new Android_Gesture_Detector() {
            @Override
            public void onLongPress(MotionEvent e) {

                //showWebMenu();
                Log.d("Longpressed",String.format("%s",e));
                show(false);

            }

            @Override
            public void onShowPress(MotionEvent e) {

                //showWebMenu();
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float vX, float vY) {
                float y1 = e1.getY(),y2=e2.getY(), x1=e1.getX(),x2=e2.getX();
                Log.d("Fling",String.format("%s %s %s %s %s %s",x1,y1,x2,y2,vX,vY));
                if (vY < -5000 && Math.abs(vX) < 1500){ // fling up
                    show();
                }
                if ( y1 < 100 & y2 < 100 && vX > 100  ){ //fling right
                    showWebMenu();
                }
                return false;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                float y1 = e1.getY(),y2=e2.getY(), x1=e1.getX(),x2=e2.getX();
                Log.d("Location",String.format("%s %s %s %s %s %s",x1,x2,y1,y2,distanceX,distanceY));
                /*if (e1.getY() < e2.getY()) {
                    Log.d("Gesture ", " Scroll Down");
                }
                if ( distanceX < -50 && Math.abs(distanceX) < 5){
                    // showWebMenu();
                }
                if (distanceY < -50) {
                    show(true);
                }*/
                show(false);
                return false;
            }
        };
// Create a GestureDetector
        mGestureDetector = new GestureDetector(this, android_gesture_detector);
        mGestureDetector.setIsLongpressEnabled(true);

        mWebView.setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View view, MotionEvent event) {
                mGestureDetector.onTouchEvent(event);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    mGestureDetector.onGenericMotionEvent(event);
                }

                return view.onTouchEvent(event);
            }
        });
        configureWebSettings(mWebView.getSettings());

        configureWebSettings(mMenuView.getSettings());
        //pickUserAccount();
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Launch();
            }
        },new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Cursor c = Downloader.query((new DownloadManager.Query()).setFilterById(intent.getExtras().getLong(DownloadManager.EXTRA_DOWNLOAD_ID)));

                if ( c.moveToFirst()){
                    int state = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
                    String filename = c.getString(c.getColumnIndex(DownloadManager.COLUMN_TITLE));
                    boolean completed = oDownloadMerger.downloadCompleted(filename);
                    if(completed){
                        Tips(filename+T(" downloaded","下载完毕"));
                    }
                }
                Log.d("Here","download complete");
            }
        },new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        Launch();
        /*mWebView.post(new Runnable() {
            @Override
            public void run() {
                setHomePage();
                Log.d("HOMEPAGE",HOMEPAGE);
                if ( HOMEPAGE != null) {  mWebView.loadUrl(HOMEPAGE); }
            }
        });*/


        mTargetView = (FrameLayout)findViewById(R.id.target_view);
        mContentView = (FrameLayout) findViewById(R.id.main_content);
        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        //findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.

        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
        // Ads initilazation

        innerObjects.add(mMenuView);
        innerObjects.add(mWebView);
        innerObjects.add(streamer);

        ads = new Ads(this);
        // Ads setup ends

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

    private void close(){
        finish();
        System.exit(0);
    }


    private void hide() {
        hide(rotated(),UI_ANIMATION_DELAY);
    }

    private void hide(boolean force,int delay){

        if ( force ) {
            // Hide UI first
            //fullscreen(false);
            //mControlsView.setVisibility(View.GONE);
            mVisible = false;

            // Schedule a runnable to remove the status and navigation bar after a delay
            mHideHandler.removeCallbacks(mShowPart2Runnable);
            mHideHandler.postDelayed(mHidePart2Runnable, delay);
        }
    }

    private boolean rotated(){
        return true; // always return true to make it always full screen
        //return getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        /*Display display = ((WindowManager) this.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int rotation = display.getRotation();
        return ( rotation == Surface.ROTATION_90 ) || ( rotation == Surface.ROTATION_270);*/
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig){
        super.onConfigurationChanged(newConfig);
        show(rotated());
    }

    private void show(){
        show(rotated());
    }


    private void show(boolean rotated) {
        // Show the system bar
        /*mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);*/
        String title=mWebView.getTitle();
        if ( title.isEmpty() || title.matches("^(http(s)?|about).*$")){
            title = getString(R.string.app_desc);
        }
        if ( ! isConnected()){
            title += T("(Offline)","(离线)");
        }
        setTitle(title);
        fullscreen(false);
       if ( rotated ) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            // Schedule a runnable to display UI elements after a delay
            //mHideHandler.removeCallbacks(mHidePart2Runnable);
            //mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
        }
        mVisible = true;

    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);

    }

    public void loadPlayer(){
        mWebView.post(new Runnable(){
            @Override
            public void run() {
                /*WebSettings ws=mWebView.getSettings();
                int v=WebSettings.LOAD_CACHE_ONLY;
                if (isConnected()) {
                    v=WebSettings.LOAD_CACHE_ELSE_NETWORK;
                }
                ws.setCacheMode(v);*/
                mWebView.loadDataWithBaseURL(HOMEPAGE, sPlayerPage, "text/html", "UTF-8", null);
                startStreamer();
            }
        });
    }

    private void startStreamer(){
        if (isConnected()){
            WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
            if ( wm.getWifiState() == WifiManager.WIFI_STATE_ENABLED ) {
                wm.getConnectionInfo().getIpAddress();
                String deviceIp = Formatter.formatIpAddress(wm.getConnectionInfo()
                        .getIpAddress());
                Log.d("WIFI:", deviceIp);
                streamer.start(deviceIp,streamerPort);
            }
        }

    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
       /* client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Fullscreen Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.example.chenches.fullscreen/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction); */
    }

    private  void Launch(){
        new findHome(this).execute();
    }

    private class findHome extends AsyncTask<Void, Void, String> {

        Activity activity;
        public findHome(Activity activity){
            this.activity =  activity;
        }

        private boolean exception = false;


        /*protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            responseView.setText("");
        }*/
        @Override
        protected String doInBackground(Void... urls) {


            AdvertisingIdClient.Info idInfo = null;
            try {
                idInfo = AdvertisingIdClient.getAdvertisingIdInfo(getApplicationContext());
            } catch (GooglePlayServicesNotAvailableException | GooglePlayServicesRepairableException | IOException e) {
                e.printStackTrace();
            }
            if ( idInfo == null ){
                androidId = Settings.Secure.getString(getContentResolver(),Settings.Secure.ANDROID_ID);
            }else {
                try {
                    androidId = idInfo.getId();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }

            Log.d("Async","started");
            String site=String.format("%s?_=%s",getString(R.string.sites), getString(R.string.app_name) /*Math.random()*/),proto=getString(R.string.homeproto);
            String homehost = getString(R.string.homehost)+"."+getString(R.string.subdomain);
            String[] candidates=new String[]{proto+homehost+site,proto+getString(R.string.backhome)+site};
            //String[] candidates=new String[]{proto+getString(R.string.homehost)+site};
            String candidate;
            loadbalance l=new loadbalance(activity);
            l.cacheDir=getFilesDir().getAbsolutePath();
            exception=l.offline=!isConnected();
            final loadbalance _l = l;

            String check="sites=",pattern= "^[^\"]+\"(.*)\"[^\"]+$";
            String[] can=l.chooseCandidates(candidates,check);
            if ( can == null ) {
                candidate = homehost;
                exception = true;
            }else{
                sJSSites=can[1];
                sSites=String.format("^.*?(%s).*$",sJSSites.replaceAll(pattern,"$1").replaceAll("\",\"","|"));
                candidate=can[0];
            }
            sSafeSite=candidate.replaceAll("(^.*?)(\\w+\\.\\w+$)","$2"); // e.g our main site containing subdomain
            sPrefix= proto+candidate;

            String control = _l.getContent(sPrefix+"/js/control.js");
            Log.d("Control",control);
            Pattern pa=Pattern.compile("\"([^\"]+)\"\\s*:\"([^\"]+)\"");
            if ( control != null && !control.isEmpty()){
                Matcher ma = pa.matcher(control);
                while ( ma.find()){
                    String name=ma.group(1);
                    if (name == null ) continue;
                    String value=ma.group(2);
                    if ( name.equals("blacklist")){
                        SSLTolerentWebViewClient.blockURL = value;
                    }else if ( name.equals("customheader")){
                        pHeaderPattern = value;
                    }

                }
            }

            new Thread(new Runnable() {
                @Override
                public void run() {
                    String policies="";
                    defaultCookie=_l.getContent(sPrefix + getString(R.string.cookiepage)+"?app="+getString(R.string.app_name));
                    String query = defaultCookie.replaceAll(";","&");
                    policies = _l.getContent(sPrefix + getString(R.string.policy)+"?"+query);
                    oSitePolicy = new SitePolicy(policies);
                }
            }).start();


            final String sPlayerPageURL = sPrefix+getString(R.string.playerPage);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    sPlayerPage = _l.getContent(sPlayerPageURL);
                    String c="";
                    String basehref=contentprovider;
                    Pattern pa = Pattern.compile("(?:src|href)=\"([^\"]+\\.(?:js|css))");
                    Matcher ma = pa.matcher(sPlayerPage);
                    String content,match;

                    while (ma.find()){
                        match = ma.group(1);
                        final String u=String.format("%s%s",sPrefix,match);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                _l.getContent(u);
                            }
                        }).start();
                    }

                    if (isConnected()){
                        basehref=sPrefix;
                        c=sPlayerPageURL+"?script=1";
                    }
                    String script=String.format("<script>notyet=function(){" +
                            "$('#nav-panel').panel().panel('open');" +
                            "};$.getScript('%s');</script>",c);

                    if (!sPlayerPage.isEmpty()) {

                        sPlayerPage = sPlayerPage
                                .replace("<head>", String.format("<base href='%s'/><head>", basehref))
                                .replace("</body>", script + "</body>")
                                .replace("notyet()", "$(window).on('unload',function(){window.SNIFFER && SNIFFER.stopStreamer();})")
                                .replaceAll("<a[^>]*?icon-refresh\"[^>]*>.*?</a>", "");
                    }
                }
            }).start();


            HOMEPAGE = sPrefix+getString(R.string.homepage);
            MENUPAGE = sPrefix+getString(R.string.menupage);
            Log.d("Async","finished:"+sSafeSite);
            return HOMEPAGE;
        }


        @Override
        protected void onPostExecute(String response) {
            if(response == null || exception) {
                //response = T("Something is not right, Please check if you have usable network connection?","连不上网络，请看看有没有网络信号？")+"<hr/>";
                //displayErrorPage(mWebView,response);
                displayErrorPage(mWebView,"");
            }else {
                Log.d("Async", "POSTED" + response);
                mWebView.loadUrl(response);
                //loadMenuPage("");
            }
            //mMenuView.loadUrl(MENUPAGE);
        }
    }
}
