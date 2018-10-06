package dmeeuwis.nakama.data;

import android.app.Application;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.util.JsonWriter;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;

import dmeeuwis.kanjimaster.BuildConfig;
import dmeeuwis.nakama.primary.Iid;
import dmeeuwis.nakama.primary.KanjiMasterApplicaton;

public class UncaughtExceptionLogger {

    static Application app;

    public static void init(KanjiMasterApplicaton kanjiMasterApplicaton) {
        app = kanjiMasterApplicaton;
    }

    public static void backgroundLogError(final String message, final Throwable ex){
        if(app == null){
            Log.e("nakama", "Error: UncaughtExceptionLogger not initialized! Can't log.", ex);
            return;
        }
        backgroundLogError(message, ex, app);
    }

    public static void backgroundLogError(final String message, final Throwable ex, final Context applicationContext){
        final Thread t = Thread.currentThread();
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                logError(t, message, ex, applicationContext);
            }
        });
    }

    private static void appendStackTrace(Throwable ex, JsonWriter jw) throws IOException {
        jw.value(ex.getMessage());
        for(StackTraceElement e: ex.getStackTrace()){
            jw.value(e.toString());
        }

        if(ex.getCause() != null){
            jw.value("--------> caused by:");
            appendStackTrace(ex.getCause(), jw);
        }
    }

    public static void logError(Thread thread, String message, Throwable ex, Context applicationContext){
        Log.e("nakama", "Logging error in background: " + "message", ex);
        try {
            Writer netWriter = new StringWriter();
            JsonWriter jw = new JsonWriter(netWriter);
            jw.setIndent("    ");

            jw.beginObject();
            jw.name("threadName");
            jw.value(thread.getName());
            jw.name("threadId");
            jw.value(String.valueOf(thread.getId()));
            jw.name("exception");
            jw.value((message == null ? "" : message + ": ") + ex.toString());
            jw.name("iid");
            jw.value(applicationContext == null ? "?" : Iid.get(applicationContext).toString());
            jw.name("version");
            jw.value(String.valueOf(BuildConfig.VERSION_CODE));
            jw.name("device");
            jw.value(Build.MANUFACTURER + ": " + Build.MODEL);
            jw.name("os-version");
            jw.value(Build.VERSION.RELEASE);

            jw.name("stack");
            jw.beginArray();
            appendStackTrace(ex, jw);
            jw.endArray();
            jw.endObject();
            jw.close();

            String json = netWriter.toString();
            Log.i("nakama", "Will try to send error report: " + json);

            if (BuildConfig.DEBUG) {
                Log.e("nakama", "Swallowing error due to DEBUG build");
                return;
            }

            URL url = HostFinder.formatURL("/write-japanese/bug-report");
            HttpURLConnection report = (HttpURLConnection) url.openConnection();
            try {
                report.setRequestMethod("POST");
                report.setDoOutput(true);
                report.setReadTimeout(10_000);
                report.setConnectTimeout(10_000);
                report.setRequestProperty("Content-Type", "application/json");
                OutputStream out = report.getOutputStream();
                try {
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
                    writer.write(json);
                    writer.close();
                } finally {
                    out.close();
                }
                int responseCode = report.getResponseCode();
                Log.i("nakama", "Response code from writing error to network: " + responseCode);
            } finally {
                report.disconnect();
            }

        } catch (Throwable e) {
            Log.e("nakama", "Error trying to report error", e);
        }
    }

    public static void backgroundLogBugReport(final String json){
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                logBugReport(json);
            }
        });
    }

    public static void logBugReport(String json){
        try {
            Log.d("nakama", "Will try to send bug report: " + json);

            if (BuildConfig.DEBUG) {
                Log.e("nakama", "Swallowing error due to DEBUG build");
                return;
            }

            URL url = HostFinder.formatURL("/write-japanese/user-bug-report");
            HttpURLConnection report = (HttpURLConnection) url.openConnection();
            try {
                report.setRequestMethod("POST");
                report.setDoOutput(true);
                report.setReadTimeout(10_000);
                report.setConnectTimeout(10_000);
                report.setRequestProperty("Content-Type", "application/json");
                OutputStream out = report.getOutputStream();
                try {
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
                    writer.write(json);
                    writer.close();
                } finally {
                    out.close();
                }
                int responseCode = report.getResponseCode();
                Log.i("nakama", "Response code from writing error to network: " + responseCode);
            } finally {
                report.disconnect();
            }

        } catch (Throwable e) {
            Log.e("nakama", "Error trying to report error", e);
        }
    }

}
