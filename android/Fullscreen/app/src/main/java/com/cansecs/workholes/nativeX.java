package com.cansecs.workholes;

/**
 * Created by chenches on 11/16/2016.
 */
// Native X
import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.nativex.monetization.MonetizationManager;
import com.nativex.monetization.business.reward.Reward;
import com.nativex.monetization.communication.RedeemCurrencyData;
import com.nativex.monetization.communication.RedeemRewardData;
import com.nativex.monetization.enums.AdEvent;
import com.nativex.monetization.enums.NativeXAdPlacement;
import com.nativex.monetization.listeners.CurrencyListenerBase;
import com.nativex.monetization.listeners.CurrencyListenerV2;
import com.nativex.monetization.listeners.OnAdEventV2;
import com.nativex.monetization.listeners.RewardListener;
import com.nativex.monetization.listeners.OnAdEvent;
import com.nativex.monetization.listeners.SessionListener;
import com.nativex.monetization.mraid.AdInfo;

public class nativeX extends _Ads {
    // Native X
    public nativeX(FullscreenActivity activity){
        super(activity);

        this.ctx = activity.getApplicationContext();
        MonetizationManager.enableLogging(true);
        MonetizationManager.createSession(this.ctx, "112819", "cansecs.com@gmail.com", sessionListener);
        MonetizationManager.setCurrencyListener(callback);
        MonetizationManager.setRewardListener(rewardListener);
        setIcon("http://www.nativex.com/wp-content/uploads/2016/11/nativex-logo2-300x130.png");
        setDescription(
                "Fast and easy, have a fair amount of choices",
                "Only can credit you one point or less a time. Need to read about the instruction before your action",
                "简单易操作，有不少的选择。在几分钟之内就可以获得积分",
                "一次只能积到较少的积分，而且需要仔细阅读英文说明，知道它需要什么操作，否则会视为无效。"
        );

    }
    private SessionListener sessionListener = new SessionListener() {
        @Override
        public void createSessionCompleted(boolean success, boolean isOfferWallEnabled, String sessionId) {
            if (success) {
                // a session with our servers was established successfully.
                // the app is now ready to show ads.
                System.out.println("Wahoo! Now I'm ready to show an ad.");
            } else {
                // establishing a session with our servers failed;
                // the app will be unable to show ads until a session is established
                System.out.println("Oh no! Something isn't set up correctly - re-read the documentation or ask customer support for some help - https://selfservice.nativex.com/Help");
            }
        }
    };
    private Context ctx;

    private RewardListener rewardListener = new RewardListener() {
        @Override
        public void onRedeem(RedeemRewardData data) {
            // take possession of the balances returned here
            int totalRewardAmount = 0;
            //data.showAlert(activity);
            for (Reward reward : data.getRewards()) {
                Log.d("Sample", "Reward: rewardName:" + reward.getRewardName()
                        + " rewardId:" + reward.getRewardId()
                        + " amount:" + Double.toString(reward.getAmount()));
                // add the reward amount to the toprivate Context ctx;tal
                totalRewardAmount += reward.getAmount();
            }
        }
    };

    CurrencyListenerBase callback = new CurrencyListenerV2() {
        @Override
        public void onRedeem(RedeemCurrencyData info) {
            Log.d("Redeem",String.valueOf(info));
            //TODO: Take control of balances: implement currency redemption for your player here
        }
    };


    NativeXAdPlacement placement = NativeXAdPlacement.Game_Launch;


    @Override
    public void run(){

        MonetizationManager.fetchAd(activity, placement, onAdEventListener);
    }

    // set this after createSession() is called

    private OnAdEventV2 onAdEventListener = new OnAdEventV2() {
        @Override
        public void onEvent(AdEvent event, AdInfo adInfo, String message) {
            System.out.println("Placement: "+adInfo.getPlacement()+"Event:"+event);
            switch (event) {
                case ALREADY_FETCHED:
                    // fetchAd() is called with an Ad Name and there is already a fetched ad with the same name ready to be shown.
                    break;
                case ALREADY_SHOWN:
                    // showAd() is called with an Ad Name and there is an ad already being shown with the same name at this moment.
                    break;
                case BEFORE_DISPLAY:
                    // Just before the Ad is displayed on the screen.
                    break;
                case DISMISSED:
                    // The ad is dismissed by the user or by the application.
                    break;
                case DISPLAYED:
                    // The ad is shown on the screen. For fetched ads this event will fire when the showAd() method is called.
                    break;
                case DOWNLOADING:
                    // fetchAd() is called with an Ad Name and there is an ad already being fetched with the same name at this moment.
                    break;
                case ERROR:
                    // An error has occurred and the ad is going to be closed.
                    // More information about the error is passed in the "message" parameter.
                    break;
                case EXPIRED:
                    // A fetched ad expires. All fetched ads will expire after a certain time period if not shown.
                    run();
                    break;
                case FETCHED:
                    // The ad is ready to be shown. For fetched ads this method means that the ad is fetched successfully.
                    // You may want to initially put the showReadyAd() call here when you're doing your initial testing,
                    // but for production you should move it to a more appropriate place, as described in the Show an Ad section.
                    MonetizationManager.showReadyAd(activity, placement, null);
                    break;
                case NO_AD:
                    // The device contacts the server, but there is no ad ready to be shown at this time.
                    break;
                case USER_NAVIGATES_OUT_OF_APP:
                    // The user clicks on a link or a button in the ad and is going to navigate out of the app
                    // to the Google Play or a browser applications.
                    break;
                case IMPRESSION_CONFIRMED:
                    // ad has its impression event fired
                    break;
                case AD_CONVERTED:
                    // rewarded video ad has converted, and rewards will be given
                    break;
                default:
                    break;
            }
        }
    };


}
