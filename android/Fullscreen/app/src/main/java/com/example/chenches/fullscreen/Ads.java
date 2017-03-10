package com.example.chenches.fullscreen;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.widget.Button;

import java.util.Random;

import static android.R.attr.name;

/**
 * Created by sam on 13/12/16.
 */


public class Ads {
    /* ads setup */
    class AdsObj{

        private String toJSONArr(String... arr){
            String ret="[";
            for (int i = 0; i < arr.length; i++) {
                ret+=arr[i]+((i==arr.length-1)?"]":",");
            }
            return ret;
        }

        @JavascriptInterface
        public String config() {
            String pp = activity.T("Purchase a plan","买个实惠");
            return toJSONArr(
                    nx.config("nativeX")
                    ,"{\"name\":\"" + pp +"\"}"
                    //,inapp.config("purchase")
            );
            /*String desc=activity.T(join("Fast and easy, have a fair amount of choices",
                    "Only can credit you one point or less a time. Need to read about the instruction before your action"),
                    join("简单易操作，有不少的选择。在几分钟之内就可以获得积分",
                    "一次只能积到较少的积分，而且需要仔细阅读英文说明，知道它需要什么操作，否则会视为无效。")),
                    inapp = activity.T("Purchase a plan","买个实惠"),
                    ssdec=activity.T(join("a","b"),join("c","d")),
                    purchasedesc=activity.T(join("You can choose any plan that suites you, and worry free.",
                            "You have spent some real dolloar here, though very small amount."
                    ),join("选择适合你的计划，随心所欲",
                            "得花真正的钱。")),arr="[%s,%s,%s,%s]";
            return String.format(arr,descObj("http://www.nativex.com/wp-content/uploads/2016/11/nativex-logo2-300x130.png",
                    desc,"nativeX"),
                    descObj("https://pbs.twimg.com/profile_images/731798993496383488/KHF85sZk_400x400.jpg",ssdec,"SuperSonic"),
                    "{\"name\":\"" + inapp +"\"}",descObj("/img/wmpicon.jpg",purchasedesc,"purchase"));
                    */
        }
        @JavascriptInterface
        public void nativeX(){
            nx.run();

        }

        @JavascriptInterface
        public void purchase(){ // in-app purchase goes here
            inapp.Postback("appid=1&revenue=mon1",null);
        }

    }
  nativeX nx;
    Inapp inapp;
  FullscreenActivity activity;
    public Ads(FullscreenActivity activity){
        this.activity = activity;
        nx = new nativeX(activity);
        inapp = new Inapp(activity);

        activity.mWebView.addJavascriptInterface(new AdsObj(),"AppAds");
        //activity.mWebView.addJavascriptInterface(new AdsObj(),"MediaSource");
        //activity.mWebView.addJavascriptInterface(new AdsObj(),"webkitMediaSource");
    }

    void run(){
        activity.mWebView.loadUrl(activity.sPrefix+"/points.html");

    }



}
