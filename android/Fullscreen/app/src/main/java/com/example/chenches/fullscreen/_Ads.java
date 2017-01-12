package com.example.chenches.fullscreen;

/**
 * Created by sam on 12/01/17.
 */

public abstract class _Ads {
    FullscreenActivity activity;
    String icon="";
    String [] description = new String[4];
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
    public _Ads(FullscreenActivity activity){
        this.activity = activity;
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


}
