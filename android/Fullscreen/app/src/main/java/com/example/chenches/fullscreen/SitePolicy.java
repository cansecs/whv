package com.example.chenches.fullscreen;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by chenches on 9/22/2016.
 */
public class SitePolicy {
    private class Site{
        String domain="",site="";
        public Site(String url){
            try {
                URL u = new URL(url);
                String host = u.getHost();
                String items[] = host.split("\\.");
                site = items[items.length-2];
                domain = String.format("%s_%s",site,items[items.length-1]);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
    }
    private class Policy{
        String UA = "";
        String getVideoSrc = "";
        String[] subDomains;
        final static String _UA="UA",_getVideoSrc="getVideoSrc",_subdomain="subdomain";

        private String getValue(JSONObject o,String k){
            String v="";
            try {
                v=o.getString(k);
            } catch (JSONException e) {
                Log.d("Ignored","No "+k+" defined in policy");
            }
            return v;
        }
        public Policy(JSONObject obj) throws JSONException {
            UA=getValue(obj,_UA);
            getVideoSrc=getValue(obj,_getVideoSrc);
            subDomains=getValue(obj,_subdomain).split(",");
        }
        public String getUA(){
            return UA;
        }
        public String getScript(){
            return getVideoSrc;
        }
        public String[] getSubDomains() { return subDomains;}
    }
    public Map<String,Object> map = new HashMap<>();
    public Map<String,String> submap = new HashMap<>();
    public String getKey(String url,boolean return_site) {
        if (map.containsKey(url)) {
            return url;
        }
        Site s = new Site(url);
        String key = "";
        if (map.containsKey(s.domain)) {
            key = s.domain;
        } else if (submap.containsKey(s.domain)) {
            key = submap.get(s.domain);
        } else if (submap.containsKey(s.site)) {
            key = submap.get(s.site);
        }
        if ( return_site){
            key=key.replaceAll("(^\\w+)_.*$","$1");
        }
        return key;
    }
    public Policy getPolicy(String url){
        return (Policy)map.get(getKey(url,false));
    }
    public String getUA(String key){
        Policy pol=getPolicy(key);
        return (pol==null)?"":pol.getUA();
    }

    public String getScript(String key){
        Policy pol=getPolicy(key);
        return (pol==null)?"":pol.getScript();
    }
    public SitePolicy(String policystring){
        try {
            JSONObject object=new JSONObject(policystring);
            Iterator<String> keysItr = object.keys();
            while(keysItr.hasNext()) {
                String key=keysItr.next();
                Object value=object.get(key);
                if (value instanceof JSONObject){
                    value = new Policy((JSONObject)value);
                    String [] subdomains= ((Policy)value).getSubDomains();
                    if (subdomains != null ){
                        for (int i = 0; i < subdomains.length; i++) {
                            submap.put(subdomains[i],key);
                        }
                    }
                }
                map.put(key,value);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
