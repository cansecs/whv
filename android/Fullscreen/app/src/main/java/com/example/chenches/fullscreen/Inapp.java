package com.example.chenches.fullscreen;

/**
 * Created by sam on 12/01/17.
 */

public class Inapp extends _Ads{

    public Inapp(FullscreenActivity activity){
        super(activity);
        setIcon("/img/wmpicon.jpg");
        setDescription(
        "You can choose any plan that suites you, and worry free.",
        "You have spent some real dolloar here, though very small amount.",
        "选择适合你的计划，随心所欲",
        "得花真正的钱。");
    }
    @Override
    public void run() {

    }
}
