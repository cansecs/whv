package com.example.chenches.fullscreen;

import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by chenches on 9/16/2016.
 */
public class PointState {
    public JSONObject pointState;
    public String user;
    public Date expire;
    private long points;
    private final static String rawcookie="whtoken";
    private final static String usercookie="cn1p3";
    private final static String cookiePattern="^.*?%s=([^;]+).*$";

    private final static String expireField = "e";
    private final static String userField = "u";
    private String cookie = "";
    public String getPoints(){
        return String.format("%.1f", ((float)Points()/ (24 * 60 * 60 * 1000))); // Format d.d day as points
    }

    public String getRaw(){
        return getCookie(rawcookie);
    }

    public String getCookie(String key){
        String cook=String.format(cookiePattern,key);
        String value = "";
        if ( cookie.matches(cook) ) {
            value = cookie.replaceAll(cook,"$1");
        }
        Log.d("Cookie Found:",cookie+":"+value);
        return value;
    }

    @Override
    public String toString(){
        return String.format("%s %s %s - %s",user,expire, Points(),getPoints());
    }
    public boolean isDemo(){
        return user != null && user.contains("demo.com");
    }
    public boolean isVip(){
        return Points() > 0;
    }
    public long Points(){
        long points=0;
        if (expire != null ) {
            points=expire.getTime()-new Date().getTime();
        }
        return points;
    }
    private boolean isValid(String value){
        return value != null && (value.startsWith("{") && value.endsWith("}"));
    }
    public void parse(String c){
        cookie = c;
        String value=getCookie(usercookie);
        if ( !isValid(value)) try {
            value = new String(Base64.decode(value, Base64.DEFAULT), "UTF-8");
        } catch (IllegalArgumentException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if ( isValid(value)  ) {
            try {
                pointState = new JSONObject(value);
                String _user=pointState.getString(userField);
                if ( ! ( _user == null || _user.isEmpty()) ) user=_user;
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
                expire = new Date();
                try {
                    expire = (Date) dateFormat.parse(pointState.getString(expireField));
                }catch (ParseException e) {
                    Log.d("PointState",String.format("Wrong date format:%s",value));
                }
            } catch (JSONException e) {
                Log.d("PointState",String.format("No Expire date:%s",value));
            }
        }
    }
    public PointState(String cookie){
        if ( ! ( cookie == null || cookie.equals(""))) {
            this.cookie=cookie;
            parse(cookie);
        }
    }
}
