package dmeeuwis.kanjimaster.ui.data;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import dmeeuwis.kanjimaster.logic.data.CharacterProgressDataHelper;
import dmeeuwis.kanjimaster.logic.data.HostFinder;
import dmeeuwis.kanjimaster.logic.util.JsonWriter;

import org.threeten.bp.LocalDateTime;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import dmeeuwis.kanjimaster.BuildConfig;
import dmeeuwis.kanjimaster.logic.data.IidFactory;
import dmeeuwis.kanjimaster.ui.sections.primary.KanjiMasterActivity;
import dmeeuwis.kanjimaster.ui.KanjiMasterApplicaton;

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
            jw.value(applicationContext == null ? "?" : IidFactory.get().toString());
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

    public static void backgroundLogOverride(final String json){
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                logOverride(json);
            }
        });
    }


    public static void logBugReport(String json){
        logJson("/write-japanese/user-bug-report", json);
    }

    public static void logJson(String path, String json){
        try {
            Log.d("nakama", "Will try to send json to " + path + ": " + json);

            URL url = HostFinder.formatURL(path);
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
            Log.e("nakama", "Error trying to log " + path + ": " + e.getMessage(), e);
        }
    }

    public static void logOverride(String json){
        logJson("/write-japanese/override-report", json);
    }


    // Amazon Support
    public static void backgroundLogPurchase(final Activity parentActivity, final String receiptId) {
        backgroundLogPurchase(parentActivity, "AmazonAppStore", receiptId);
    }

    public static void backgroundLogPurchase(final Activity parentActivity, final String store, final String token) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                backgroundLogPurchaseImpl(parentActivity, store, token);
            }
        });
    }

    private static void backgroundLogPurchaseImpl(Activity parentActivity, String store, String token) {
        try {
            StringWriter sw = new StringWriter();
            JsonWriter jw = new JsonWriter(sw);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(app.getApplicationContext());
            CharacterProgressDataHelper dbHelper = new CharacterProgressDataHelper(IidFactory.get());

            jw.beginObject();

                jw.name("iid").value(IidFactory.get().toString());
                jw.name("installed").value(prefs.getString("installTime", null));
                jw.name("purchased").value(LocalDateTime.now().toString());
                jw.name("purchaseToken").value(token);
                jw.name("store").value(store);

                jw.name("charsetLogs");
                jw.beginObject();

                    Map<String, String> practiceCounts = dbHelper.countPracticeLogs();
                    for(Map.Entry<String, String> e: practiceCounts.entrySet()){
                        jw.name(e.getKey()).value(e.getValue());
                    }

                jw.endObject();

                if(parentActivity instanceof KanjiMasterActivity){
                    KanjiMasterActivity a = (KanjiMasterActivity)parentActivity;
                    jw.name("appState");
                    a.stateLog(jw);
                }

            jw.endObject();

            logJson("/write-japanese/purchase", sw.toString());
        } catch (Throwable e) {
            UncaughtExceptionLogger.backgroundLogError("Error logging purchase", e);
        }
    }
}
