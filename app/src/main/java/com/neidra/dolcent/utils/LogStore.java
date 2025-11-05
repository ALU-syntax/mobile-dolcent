package com.neidra.dolcent.utils;

import android.content.Context;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class LogStore {
    private static final String DIR = "print-logs";
    private static final long MAX_FILE_BYTES = 512 * 1024; // 512 KB per file
    private static volatile LogStore sInstance;
    private final File dir;
    private final SimpleDateFormat dayFmt = new SimpleDateFormat("yyyyMMdd", Locale.US);
    private final SimpleDateFormat tsFmt  = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);

    private LogStore(Context ctx) {
        this.dir = new File(ctx.getFilesDir(), DIR);
        if (!dir.exists()) dir.mkdirs();
    }

    public static LogStore get(Context ctx) {
        if (sInstance == null) {
            synchronized (LogStore.class) {
                if (sInstance == null) sInstance = new LogStore(ctx.getApplicationContext());
            }
        }
        return sInstance;
    }

    /** Tulis 1 baris log (akan auto-rotate per file & per hari) */
    public synchronized void write(String tag, String msg) {
        try {
            String fn = "log_" + dayFmt.format(new Date()) + ".log";
            File f = new File(dir, fn);

            // rotate kalau kebesaran
            if (f.exists() && f.length() > MAX_FILE_BYTES) {
                File rotated = new File(dir, "log_" + dayFmt.format(new Date()) + "_" + System.currentTimeMillis() + ".log");
                // rename → mulai file baru
                //noinspection ResultOfMethodCallIgnored
                f.renameTo(rotated);
                f = new File(dir, fn);
            }

            try (FileWriter fw = new FileWriter(f, true);
                 BufferedWriter bw = new BufferedWriter(fw)) {
                String line = tsFmt.format(new Date()) + " [" + tag + "] " + msg;
                bw.write(line);
                bw.newLine();
                bw.flush();
            }
        } catch (Exception ignored) {}
    }

    public File[] listLogFiles() {
        File[] all = dir.listFiles((d, name) -> name.endsWith(".log"));
        return all != null ? all : new File[0];
    }

    public File getDirectory() { return dir; }
}