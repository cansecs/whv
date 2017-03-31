package com.cansecs.workholes;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Created by sam on 06/01/17.
 */

public class whvcontent extends ContentProvider {
    public static String subdir ="";
    public boolean onCreate(){
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] strings, String s, String[] strings1, String s1) {
        return null;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        String path = uri.getPath();
        String ret="text/html";
        if ( path.endsWith(".js")){
            ret="text/javascript";
        }else if (path.endsWith(".css")){
            ret="text/css";
        }else if (path.endsWith(".mp4")){
            ret="video/mp4";
        }else{
            Log.d("UnknownTYPE",path);
        }
        return ret;

    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        return null;
    }

    @Override
    public int delete(Uri uri, String s, String[] strings) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String s, String[] strings) {
        return 0;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) {
        String path= uri.getPath();
        Log.d("Content:","->"+uri+" =>"+path);

        ParcelFileDescriptor parcel = null;
        File file = new File(path);
        try{

            if (!file.exists()){

                file = new File(loadbalance.cacheDir,loadbalance.flatten(path));
                Log.d("Contenthere",file.getAbsolutePath());
            }else{
                Log.d("Contentright",file.getAbsolutePath());
            }
                parcel = ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY);
            } catch (FileNotFoundException e) {
                Log.d("contenterr:",String.format("%s->%s",file.getAbsolutePath(),file.exists()));
                e.printStackTrace();
            }
        return parcel;
    }

}
