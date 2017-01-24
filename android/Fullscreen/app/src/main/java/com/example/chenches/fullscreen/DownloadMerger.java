package com.example.chenches.fullscreen;

import android.app.DownloadManager;
import android.database.Cursor;
import android.util.Log;
import android.util.SparseArray;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by sam on 12/01/17.
 */

public class DownloadMerger {
    class DF{
        int length = 1;
        String _filename,realname;
        final Pattern segements = Pattern.compile("^(.*?)_(\\d+)_of_(\\d+)(.*?)$");
        SparseArray<String> fragments=new SparseArray<>();
        //Map<Integer,File> fragments=new HashMap<>();
        public String getName(){
            if (realname != null){
                return realname;
            }else{
                return _filename;
            }
        }

        public boolean hasFragments(){
            return this.length > 1;
        }

        boolean hasAll(){
            return (length == 1) || ( fragments.size() == length);
        }

        public int getLength(){
            return length;
        }
        public DF(String filename){
            parse(filename);
        }

        public void add(String filename){
            parse(filename);
        }

        void parse(String filename){
            this._filename = filename;
            Matcher ma = segements.matcher(filename);
            boolean found=false;
            while (ma.find()){
                found=true;
                realname=ma.group(1)+ma.group(4);
                int idx=Integer.parseInt(ma.group(2));
                //fragments.put(idx,filename);
                fragments.put(idx,filename);
                length=Integer.parseInt(ma.group(3));
            }
            if (!found){
                fragments.put(0,filename);
            }
        }
    }

    final static int BUFSZ = 1024*1024;
    static boolean fullscan = true; // only the first time it does full scan


    FullscreenActivity activity;
    DownloadManager Downloader;
    File DownloadDir;

    final boolean MergeFiles(File target,List<File> sources){
        boolean success = false;
        try {
            BufferedWriter fw = new BufferedWriter(new FileWriter(target));
            for (File source:sources
                 ) {
                BufferedReader br = new BufferedReader((new FileReader(source)));

                    char[] buffer = new char[BUFSZ];
                    int len=0;
                    while ( ( len = br.read(buffer)) >= 0){
                        fw.write(buffer,0,len);
                    }
                br.close();
                boolean deleted = source.delete();
                if (!deleted){
                    Log.d("Cannot Delete",source.getName());
                }
            }
            fw.close();
        } catch (IOException e) {
            success=false;
            e.printStackTrace();
        }
        return success;
    }
    public DownloadMerger(FullscreenActivity activity){
        this.activity=activity;
        this.Downloader = activity.Downloader;
        this.DownloadDir = activity.sDownloaddir;
    }

    public Map<String,Object> scanFolder() {
        return listFolder(null,false);
    }

    public Map<String,Object> listFolder(String pattern,boolean checkonly){
            final String pa=pattern;
            FilenameFilter ff = new FilenameFilter() {
                @Override
                public boolean accept(File file, String s) {
                    if ( pa == null || pa.isEmpty()) return true;
                    return s.matches(pa);
                }
            };
            Map<String,Object> Pool = new HashMap<>();
            ArrayList<String> lists=new ArrayList<String>();
            if ( DownloadDir ==null || !DownloadDir.exists()) return Pool;
            File direct = DownloadDir;

            if (direct.exists()) {
                File [] files = direct.listFiles(ff);
                for (File file : files) {
                    String filename = file.getName();
                    DF tmpdf=new DF(filename);
                    String realname=tmpdf.getName();
                    Object o = Pool.get(realname);
                    DF df;
                    if ( o == null ){
                        df = tmpdf;
                        Pool.put(realname,df);
                    }else {
                        df = (DF) o;
                        df.add(filename);
                    }
                }
                Map<String,Object> tmpPool=new HashMap<>();
                for (String key: Pool.keySet()  ) {
                    DF df=(DF)Pool.get(key);
                    int size=df.fragments.size();
                    boolean done = df.hasAll();
                    if (checkonly){
                        tmpPool.put(key,done);
                    }else {
                        File[] fs = new File[size];
                        for (int i = 0; i < size; i++) {
                            fs[i] = new File(DownloadDir, df.fragments.get(i));
                        }
                        tmpPool.put(key, fs);
                    }
                }
                Pool=tmpPool;
            }
        return Pool;
    }
    public boolean downloadCompleted(String filename){
        DF df=new DF(filename.replaceAll("\\.\\w+$",""));
        String name=df.getName();
        String pattern="^.*?"+name+".*$";
        Map<String,Object> ret=listFolder(pattern,true);
        for (String key:ret.keySet()){
            if (name.startsWith(key)){
                return (boolean)ret.get(key);
            }
        }
        return false;
    }

}
