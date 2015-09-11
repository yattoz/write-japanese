package dmeeuwis.nakama.data;

import android.app.Activity;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import dmeeuwis.nakama.primary.Iid;
import dmeeuwis.util.Util;

public class PracticeLogSync {

    final private static String SERVER_SYNC_PREFS_KEY = "last-progress-server-sync-time";
    final private static String DEVICE_SYNC_PREFS_KEY = "last-progress-server-sync-time";
    final private static String SYNC_URL = "http://192.168.1.99:8080/write-japanese/progress-sync";

    final Activity activity;

    public PracticeLogSync(Activity activity) {
        this.activity = activity;
    }

    public void sync() throws IOException {
        URL syncUrl;
        String iid = Iid.get(activity.getApplication()).toString();
        try {
            syncUrl = new URL(SYNC_URL);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
        String lastSyncServerTimestamp = prefs.getString(SERVER_SYNC_PREFS_KEY, "2000-01-01 00:00:00 +00");
        String lastSyncDeviceTimestamp = prefs.getString(DEVICE_SYNC_PREFS_KEY, "2000-01-01 00:00:00 +00");

        HttpURLConnection urlConnection = (HttpURLConnection) syncUrl.openConnection();
        urlConnection.setDoOutput(true);
        urlConnection.setDoInput(true);
        urlConnection.setRequestMethod("POST");
        urlConnection.setRequestProperty("Content-Type", "application/json");

        Writer netWriter = new StringWriter();
        JsonWriter jw = new JsonWriter(netWriter);

        jw.beginObject();
        jw.name("install_id").value(iid);

        WriteJapaneseOpenHelper db = new WriteJapaneseOpenHelper(this.activity.getApplicationContext());
        SQLiteDatabase sqlite = db.getReadableDatabase();
        try {
            jw.name("prev-sync-timestamp").value(lastSyncServerTimestamp);
            Cursor c = sqlite.rawQuery("SELECT * FROM practice_log WHERE timestamp > ? AND install_id = ?", new String[]{lastSyncDeviceTimestamp, iid});
            try {
                // stream over all rows since that time
                jw.name("practice_logs");
                jw.beginArray();
                while (c.moveToNext()) {
                    jw.beginObject();
                    for (int i = 0; i < c.getColumnCount(); i++) {
                        Log.i("nakama", "Reading column " + i + ": " + c.getColumnName(i));
                        Log.i("nakama", "Reading column value " + c.getString(i));
                        jw.name(c.getColumnName(i));
                        jw.value(c.getString(i));
                    }
                    jw.endObject();
                }
            } finally {
                c.close();
            }
            jw.endArray();
            jw.endObject();
            jw.close();
            String jsonPost = netWriter.toString();
            Log.i("nakama", "Posting JSON sync: " + jsonPost);

            OutputStream out = urlConnection.getOutputStream();
            try {
                out.write(jsonPost.getBytes("UTF-8"));
            } finally {
                out.close();
            }

            // stream over all rows in from POST response
            InputStream inStream = urlConnection.getInputStream();
            String jsonResponse = Util.slurp(inStream);
            Log.i("nakama-sync", "Saw progress-sync response JSON: " + jsonResponse);

            Reader rin = new InputStreamReader(new ByteArrayInputStream(jsonResponse.getBytes("UTF-8")));
            JsonReader jr = new JsonReader(rin);

            jr.beginObject();
            String syncTimestampName = jr.nextName();
            String syncTimestampValue = jr.nextString();
            Log.i("nakama-sync", "Saw JSON response object sync values: " + syncTimestampName + " = " + syncTimestampValue);

            SharedPreferences.Editor e = prefs.edit();
            e.putString(DEVICE_SYNC_PREFS_KEY, syncTimestampValue);
            e.putString(SERVER_SYNC_PREFS_KEY, syncTimestampValue);
            e.apply();

            Map<String, String> values = new HashMap<>();
            jr.nextName();      // "practice_logs" key
            jr.beginArray();
            while (jr.hasNext()) {
                jr.beginObject();
                while (jr.hasNext()) {
                    values.put(jr.nextName(), jr.nextString());
                    try {
                        String[] insert = new String[]{values.get("id"), values.get("install_id"),
                                values.get("charset"), values.get("character"), values.get("score"),
                                values.get("timestamp")};
                        Log.i("nakama-sync", "Inserting remove log: " + Util.join(", ", insert));
                        DataHelper.selectRecord(sqlite, "INSERT INTO practice_log(install_id, id, charset, character, score, timestamp", insert);
                    } catch (RuntimeException t) {
                        if (t.getCause() instanceof SQLException) {
                            Log.e("nakama", "DB error while error inserting sync log: " + Arrays.toString(values.entrySet().toArray()));
                        } else {
                            throw t;
                        }
                    }
                }
                jr.endObject();
            }
            jr.endArray();
            jr.endObject();
            jr.close();
            rin.close();
            inStream.close();

            Log.i("nakama-sync", "Sync complete!");

        } finally {
            db.close();
            sqlite.close();
        }
    }
}