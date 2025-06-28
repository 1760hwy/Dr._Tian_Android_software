// SimpleImageLoader.java
package com.example.myapplication;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

/**
 * 一个极简的网络图片加载器，内部做了简单缓存。
 */
public class SimpleImageLoader {
    // 内存缓存
    private static final HashMap<String, Bitmap> cache = new HashMap<>();

    public interface Callback {
        void onLoaded(Bitmap bitmap);
    }

    public static void load(String url, Callback cb) {
        // 如果内存中已有，直接回调
        if (cache.containsKey(url)) {
            cb.onLoaded(cache.get(url));
            return;
        }
        new AsyncTask<String, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(String... params) {
                try {
                    HttpURLConnection conn = (HttpURLConnection)
                            new URL(params[0]).openConnection();
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    InputStream in = conn.getInputStream();
                    return BitmapFactory.decodeStream(in);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
            @Override
            protected void onPostExecute(Bitmap bmp) {
                if (bmp != null) {
                    cache.put(url, bmp);
                }
                cb.onLoaded(bmp);
            }
        }.execute(url);
    }
}
