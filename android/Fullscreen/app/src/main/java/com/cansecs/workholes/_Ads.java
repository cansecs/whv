package com.cansecs.workholes;

import android.os.AsyncTask;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by sam on 12/01/17.
 */

public abstract class _Ads {
    FullscreenActivity activity;
    String icon="";
    String [] description = new String[4];

    private class postResult extends AsyncTask<String, Void, Boolean> {

        private boolean _postBack(String query){
            boolean success=false;
            HttpURLConnection connection = null;
            if ( query == null || query.isEmpty()) return success;
            String postback=activity.sPrefix+activity.getString(R.string.postback);
            try {
                String extra=String.format("postback=%s%%26%s&type=%s",activity.androidId,activity.pointState.getRaw(),type);
                String finalurl = String.format("%s?%s&%s",postback,query,extra);
                URL url = new URL(finalurl);

                String charset = "UTF-8";
                connection = (HttpURLConnection) url.openConnection();
                Map<String,String> finalHeaders = new HashMap();
                String ua=String.format("%s:%s",query.hashCode(),(new Date()).getTime()*2);
                finalHeaders.put("user-agent",ua);
                for (String key : finalHeaders.keySet()) {
                    connection.setRequestProperty(key, finalHeaders.get(key));
                }
                int status = connection.getResponseCode();
                success = (status == HttpURLConnection.HTTP_OK);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                if ( connection != null){
                    connection.disconnect();
                }
            }
            return success;
        }

        @Override
        protected Boolean doInBackground(String... voids) {
            String query=voids[0];
            return _postBack(query);
        }

        @Override
        protected void onPostExecute(Boolean result){
            Postback(null,result);
        }
    }
    protected String getDescription(){
        return activity.T(join(description[0],description[1]),join(description[2],description[3]));
    }
    protected void setIcon(String icon){
        this.icon = icon;
    }
    protected void setDescription(String adve,String dise,String advc,String disc){
        description[0]=adve;
        description[1]=dise;
        description[2]=advc;
        description[3]=disc;
    }
    protected String type="";
    public _Ads(FullscreenActivity activity){
        this.activity = activity;
        type=this.getClass()
                .getSimpleName().toLowerCase();
    }
    protected String descObj(String icon,String desc,String url){
        return String.format(activity.getString(R.string.adsdesc),icon,desc,url);
    }
    protected String join(String a,String b){
        return String.format("%s\",\"%s",a,b);
    }
    public String config(String name){
        return descObj(icon,getDescription(),name);
    }
    abstract public void run();
    public void postbackHandler(boolean success){

    }
    public void Postback(String query,Boolean result){
        if (query == null && result != null) {
            postbackHandler(result.booleanValue());
        }
        else{
            new postResult().execute(query);
        }
    }



}
