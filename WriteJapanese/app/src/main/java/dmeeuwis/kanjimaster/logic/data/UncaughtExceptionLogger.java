package dmeeuwis.kanjimaster.logic.data;

import android.os.AsyncTask;
import android.util.Log;

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

import dmeeuwis.kanjimaster.logic.util.JsonWriter;

public abstract class UncaughtExceptionLogger {

    public static void backgroundLogError(final String message, final Throwable ex){
        final Thread t = Thread.currentThread();
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                logError(t, message, ex);
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

    public static void logError(Thread thread, String message, Throwable ex){
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
            jw.value(IidFactory.get().toString());
            jw.name("version");
            jw.value(String.valueOf(SettingsFactory.get().version()));
            jw.name("device");
            jw.value(SettingsFactory.get().device());
            jw.name("os-version");
            jw.value(SettingsFactory.get().osVersion());

            jw.name("stack");
            jw.beginArray();
            appendStackTrace(ex, jw);
            jw.endArray();
            jw.endObject();
            jw.close();

            String json = netWriter.toString();
            Log.i("nakama", "Will try to send error report: " + json);

            if (SettingsFactory.get().debug()) {
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
    public static void backgroundLogPurchase(final String receiptId) {
        backgroundLogPurchase("AmazonAppStore", receiptId);
    }

    public static void backgroundLogPurchase(final String store, final String token) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                backgroundLogPurchaseImpl(store, token);
            }
        });
    }

    private static void backgroundLogPurchaseImpl(String store, String token) {
        try {
            StringWriter sw = new StringWriter();
            JsonWriter jw = new JsonWriter(sw);

            CharacterProgressDataHelper dbHelper = new CharacterProgressDataHelper(IidFactory.get());
            jw.beginObject();

            jw.name("iid").value(IidFactory.get().toString());
            jw.name("installed").value(SettingsFactory.get().getInstallDate().toString());
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

//          TODO: break appState into own class, OS dependant
//            jw.name("appState");
//            a.stateLog(jw);

            jw.endObject();

            logJson("/write-japanese/purchase", sw.toString());
        } catch (Throwable e) {
            UncaughtExceptionLogger.backgroundLogError("Error logging purchase", e);
        }
    }
}
