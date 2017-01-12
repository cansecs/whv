package com.example.chenches.fullscreen;

import android.app.Activity;
import android.util.Log;

import com.supersonic.adapters.supersonicads.SupersonicConfig;
import com.supersonic.mediationsdk.logger.SupersonicError;
import com.supersonic.mediationsdk.model.Placement;
import com.supersonic.mediationsdk.sdk.RewardedVideoListener;
import com.supersonic.mediationsdk.sdk.Supersonic;
import com.supersonic.mediationsdk.sdk.SupersonicFactory;
import com.supersonic.mediationsdk.sdk.OfferwallListener;


import java.util.HashMap;
import java.util.Map;

/**
 * Created by sam on 10/01/17.
 */

public class supersonic extends _Ads {

    //Import the Offerwall Listener
    protected static String mUserId = "demo";
    private static final String mAppKey = "5cbca145";
    private static final String customKey = "whtoken";
    private static String customValue = "";

    public void setCustomValue(String value){
        customValue=value;
    }

    public void setmUserId(String id){
        mUserId=id;
    }

    //Import the Rewarded Video Listener

    RewardedVideoListener mRewardedVideoListener = new RewardedVideoListener()
    {
        //Invoked when initialization of RewardedVideo has finished successfully.
        @Override
        public void onRewardedVideoInitSuccess() {
            Log.d("video","success");
            mMediationAgent.showRewardedVideo();
        }
        //Invoked when RewardedVideo initialization process has failed.
        //SupersonicError contains the reason for the failure.
        @Override
        public void onRewardedVideoInitFail(SupersonicError se) {

            //Retrieve details from a SupersonicError object.
            Log.d("Init",String.format("Failed:%s",se));
            int errorCode =  se.getErrorCode();
            String errorMessage = se.getErrorMessage();
            if (errorCode == SupersonicError.ERROR_CODE_GENERIC){
                //Write a Handler for specific error's.
            }
        }

        //Invoked when RewardedVideo call to show a rewarded video has failed
        //SupersonicError contains the reason for the failure.
        @Override
        public void onRewardedVideoShowFail(SupersonicError se) {
            Log.d("video","showFailed");
        }
        //Invoked when the RewardedVideo ad view has opened.
        //Your Activity will lose focus. Please avoid performing heavy
        //tasks till the video ad will be closed.
        @Override
        public void onRewardedVideoAdOpened() {
        }
        //Invoked when the RewardedVideo ad view is about to be closed.
        //Your activity will now regain its focus.
        @Override
        public void onRewardedVideoAdClosed() {
        }
        //Invoked when there is a change in the ad availability status.
        //@param - available - value will change to true when rewarded videos are available.
        //You can then show the video by calling showRewardedVideo().
        //Value will change to false when no videos are available.
        @Override
        public void onVideoAvailabilityChanged(boolean available) {
            //Change the in-app 'Traffic Driver' state according to availability.
        }
        //Invoked when the video ad starts playing.
        @Override
        public void onVideoStart() {
        }
        //Invoked when the video ad finishes playing.
        @Override
        public void onVideoEnd() {
        }
        //Invoked when the user completed the video and should be rewarded.
        //If using server-to-server callbacks you may ignore this events and wait for
        //the callback from the Supersonic server.
        //@param - placement - the Placement the user completed a video from.
        @Override
        public void onRewardedVideoAdRewarded(Placement placement) {

            //TODO - here you can reward the user according to the given amount.
            String rewardName = placement.getRewardName();
            int rewardAmount = placement.getRewardAmount();
        }
    };
    OfferwallListener mOfferwallListener = new OfferwallListener()
    {
        /**
         * Invoked when the Offerwall is prepared and ready to be shown to the user
         */
        @Override
        public void onOfferwallInitSuccess(){
            Log.d("Offer","success");
            mMediationAgent.showOfferwall("Home_Screen");
        }
        /**
         * Invoked when the Offerwall does not load
         */
        @Override
        public void onOfferwallInitFail(SupersonicError error){
            Log.d("Offer","failed");
        }
        /**
         * Invoked when the Offerwall successfully loads for the user, after calling the 'showOfferwall' method
         */
        @Override
        public void onOfferwallOpened(){
            Log.d("Offer","opened");
        }
        /**
         * Invoked when the method 'showOfferWall' is called and the OfferWall fails to load.  //@param supersonicError - A SupersonicError Object which represents the reason of 'showOfferwall' failure.
         */
        @Override
        public void onOfferwallShowFail(SupersonicError supersonicError){
            Log.d("Offer","showfail");
        }
        /**
         * Invoked each time the user completes an Offer.
         * Award the user with the credit amount corresponding to the value of the ‘credits’
         * parameter.
         * @param credits - The number of credits the user has earned.
         * @param totalCredits - The total number of credits ever earned by the user.
         * @param totalCreditsFlag - In some cases, we won’t be able to provide the exact
         * amount of credits since the last event (specifically if the user clears
         * the app’s data). In this case the ‘credits’ will be equal to the ‘totalCredits’,
         * and this flag will be ‘true’.
         * @return boolean - true if you received the callback and rewarded the user,
         * otherwise false.
         */
        @Override
        public boolean onOfferwallAdCredited(int credits, int totalCredits, boolean totalCreditsFlag){
            Log.d("Offer",String.format("%s-%s-%s",credits,totalCredits,totalCreditsFlag));
            return true;
        }
        /**
         * Invoked when the method 'getOfferWallCredits' fails to retrieve
         * the user's credit balance info.
         * @param supersonicError - A SupersonicError object which represents the reason of 'getOffereallCredits' failure.
         * If using client-side callbacks to reward users, it is mandatory to return true on this event
         */
        @Override
        public void onGetOfferwallCreditsFail(SupersonicError supersonicError){
            Log.d("Offer","ReditFail");
        }
        /**
         * Invoked when the user is about to return to the application after closing
         * the Offerwall.
         */
        @Override
        public void onOfferwallClosed(){
            Log.d("Offer","OfferClosed");
        }
    };


/**
 * Invoked when the method 'showOfferWall' is called and the OfferWall fails to load.  //@param supersonicError - A SupersonicError Object which represents the reason of 'showOfferwall' failure.*/
    private Supersonic mMediationAgent;

    public supersonic(FullscreenActivity activity){
        super(activity);
        mMediationAgent = SupersonicFactory.getInstance();
        mMediationAgent.setOfferwallListener(mOfferwallListener);
        mMediationAgent.setRewardedVideoListener(mRewardedVideoListener);
        activity.innerObjects.add(this);
        setIcon("https://pbs.twimg.com/profile_images/731798993496383488/KHF85sZk_400x400.jpg");
        setDescription("a",
                "b",
                "c",
                "d"
        );
    }

    void onResume(){
        if (mMediationAgent != null) {
            mMediationAgent.onResume (this.activity);
        }
    }

    protected void onPause() {
        if (mMediationAgent != null) {
            mMediationAgent.onPause(this.activity);
        }
    }

    @Override
    public void run(){
        _run(1);
    }

    protected void _run(int type){
        setCustomValue(activity.pointState.getRaw());
        setmUserId(activity.androidId);

        Map<String, String> owParams = new HashMap<String, String>();
        owParams.put(customKey,customValue);
        SupersonicConfig.getConfigObj().setOfferwallCustomParams(owParams);

        //You can set optional parameters as well via the .getConfigObj
        SupersonicConfig.getConfigObj().setClientSideCallbacks(true);

        mMediationAgent.setDynamicUserId(mUserId);
        switch(type) {
            case 0:{
                //Init Offerwall
                mMediationAgent.initOfferwall(this.activity, mAppKey, mUserId);
                break;
            }
            case 1:{
                mMediationAgent.initRewardedVideo(this.activity, mAppKey, mUserId);
                break;
            }
        }


    }
}
