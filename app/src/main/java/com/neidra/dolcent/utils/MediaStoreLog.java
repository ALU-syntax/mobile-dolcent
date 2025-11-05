package com.neidra.dolcent.utils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

// MediaStoreLog.java
public final class MediaStoreLog {
    private static final String DIR = "UdDjayaLog";
    private static Uri cachedTodayUri;

    /** Dapatkan (atau buat) file log hari ini di Downloads, lalu append 1 baris. */
    public static void append(Context ctx, String line) {
        try {
            Uri uri = getOrCreateTodayUri(ctx);
            if (uri == null) return;
            try (OutputStream os = ctx.getContentResolver().openOutputStream(uri, "wa")) { // "wa" = write-append
                if (os != null) {
                    os.write((line + "\n").getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }
            }
        } catch (Exception ignored) {}
    }

    private static Uri getOrCreateTodayUri(Context ctx) {
        String today = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
        String name = "log_" + today + ".log";

        // pakai cache kalau masih hari yg sama
        if (cachedTodayUri != null) return cachedTodayUri;

        ContentResolver cr = ctx.getContentResolver();

        // Coba cari file-nya kalau sudah ada
        String[] proj = { MediaStore.Downloads._ID, MediaStore.Downloads.DISPLAY_NAME };
        String sel = MediaStore.Downloads.DISPLAY_NAME + "=? AND " +
                MediaStore.Downloads.RELATIVE_PATH + "=?";
        String relPath = "Download/" + DIR + "/";
        try (Cursor c = cr.query(MediaStore.Downloads.EXTERNAL_CONTENT_URI, proj, sel,
                new String[]{ name, relPath }, null)) {
            if (c != null && c.moveToFirst()) {
                long id = c.getLong(0);
                cachedTodayUri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id);
                return cachedTodayUri;
            }
        } catch (Exception ignored) {}

        // Belum ada → buat baru
        ContentValues v = new ContentValues();
        v.put(MediaStore.Downloads.DISPLAY_NAME, name);
        v.put(MediaStore.Downloads.MIME_TYPE, "text/plain");
        v.put(MediaStore.Downloads.RELATIVE_PATH, relPath);
        try {
            cachedTodayUri = cr.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
        } catch (Exception e) {
            cachedTodayUri = null;
        }
        return cachedTodayUri;
    }
}