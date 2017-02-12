package dmeeuwis.nakama.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.JsonWriter;
import android.util.Log;

import org.json.JSONObject;

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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dmeeuwis.kanjimaster.BuildConfig;
import dmeeuwis.nakama.LockCheckerIabHelper;
import dmeeuwis.nakama.primary.Iid;
import dmeeuwis.util.Util;

public class PracticeLogSync {

    final public static String SERVER_SYNC_PREFS_KEY = "progress-server-sync-time";
    final public static String DEVICE_SYNC_PREFS_KEY = "progress-device-sync-time";
    final private static String SYNC_URL = "https://dmeeuwis.com/write-japanese/progress-sync";
    final private static boolean DEBUG_NETWORK_SYNC = true;

    final Context context;

    public PracticeLogSync(Context c) {
        this.context = c;
    }

    public void debugPrintLog(){
        WriteJapaneseOpenHelper db = new WriteJapaneseOpenHelper(context);
        SQLiteDatabase sqlite = db.getReadableDatabase();

        List<Map<String, String>> all = DataHelper.selectRecords(sqlite, "SELECT * FROM practice_log");
        for(Map<String, String> r: all){
            Log.i("nakama-sync", new JSONObject(r).toString());
        }
    }

    public void clearSync(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor e = prefs.edit();
        e.putString(SERVER_SYNC_PREFS_KEY, "2000-01-01 00:00:00 +00");
        e.putString(DEVICE_SYNC_PREFS_KEY, "0");
        e.apply();
    }

    public void sync() throws IOException {
        URL syncUrl;
        String iid = Iid.get(context).toString();
        try {
            if(BuildConfig.DEBUG && DEBUG_NETWORK_SYNC){
                syncUrl = new URL("http://192.168.1.99:8080/write-japanese/progress-sync");
            } else {
                syncUrl = new URL(SYNC_URL);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String lastSyncServerTimestamp = prefs.getString(SERVER_SYNC_PREFS_KEY, "2000-01-01 00:00:00 +00");
        String lastSyncDeviceTimestamp = prefs.getString(DEVICE_SYNC_PREFS_KEY, "0");
        Log.i("nakama-sync", "Doing sync with last-server-sync: " + lastSyncServerTimestamp + "; last device sync: " + lastSyncDeviceTimestamp);

        HttpURLConnection urlConnection = (HttpURLConnection) syncUrl.openConnection();
        urlConnection.setDoOutput(true);
        urlConnection.setDoInput(true);
        urlConnection.setRequestMethod("POST");
        urlConnection.setRequestProperty("Content-Type", "application/json");

        Writer netWriter = new StringWriter();
        JsonWriter jw = new JsonWriter(netWriter);

        jw.beginObject();
        jw.name("install_id").value(iid);
        jw.name("level").value(LockCheckerIabHelper.getPurchaseStatus(prefs).toString());

        WriteJapaneseOpenHelper db = new WriteJapaneseOpenHelper(context);
        SQLiteDatabase sqlite = db.getReadableDatabase();
        try {
            jw.name("prev_sync_timestamp").value(lastSyncServerTimestamp);

            queryToJsonArray("practice_logs", sqlite,
                    "SELECT id, install_id, character, charset, timestamp, score, drawing " +
                            "FROM practice_log WHERE timestamp > ? AND install_id = ?",
                    new String[]{lastSyncDeviceTimestamp, iid}, jw);

            queryToJsonArray("charset_goals", sqlite,
                    "SELECT charset, timestamp, goal_start, goal FROM charset_goals WHERE timestamp > ?",
                    new String[]{lastSyncDeviceTimestamp}, jw);

            queryToJsonArray("kanji_stories", sqlite,
                    "SELECT character, story, timestamp FROM kanji_stories WHERE timestamp > ?",
                    new String[]{lastSyncDeviceTimestamp}, jw);

            queryToJsonArray("character_set_edits", sqlite,
                    "SELECT id, charset_id, name, description, characters, timestamp as device_timestamp, deleted FROM character_set_edits WHERE timestamp > ? AND install_id = ?",
                    new String[]{lastSyncDeviceTimestamp, iid}, jw);

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
            Log.i("nakama", "Received response to JSON sync: " + urlConnection.getResponseMessage() + urlConnection.getResponseMessage());
            InputStream inStream = urlConnection.getInputStream();
            String jsonResponse = Util.slurp(inStream);
            largeLog("nakama-sync", "Saw progress-sync response JSON: " + jsonResponse);

            Reader rin = new InputStreamReader(new ByteArrayInputStream(jsonResponse.getBytes("UTF-8")));
            JsonReader jr = new JsonReader(rin);

            jr.beginObject();
            String syncTimestampName = jr.nextName();
            String syncTimestampValue = jr.nextString();
            Log.i("nakama-sync", "Saw JSON response object sync values: " + syncTimestampName + " = " + syncTimestampValue);

            SharedPreferences.Editor e = prefs.edit();
            e.putString(DEVICE_SYNC_PREFS_KEY, DataHelper.selectString(sqlite,
                    "SELECT MAX(timestamp) FROM " +
                                "(SELECT MAX(timestamp) as timestamp FROM practice_log UNION " +
                                 "SELECT MAX(timestamp) as timestamp FROM charset_goals UNION " +
                                 "SELECT MAX(timestamp) as timestamp FROM kanji_stories UNION " +
                                 "SELECT MAX(timestamp) as timestamp FROM character_set_edits)"  ));

            e.putString(SERVER_SYNC_PREFS_KEY, syncTimestampValue);
            e.apply();
            Log.i("nakama-sync", "Recording device-sync timestamp as " + prefs.getString(DEVICE_SYNC_PREFS_KEY, "MISSED!"));

            String n = jr.nextName();      // "practice_logs" key
            Log.i("nakama-sync", "Expecting practice_logs... saw " + n);
            jr.beginArray();
            while (jr.hasNext()) {
                Map<String, String> values = new HashMap<>();
                jr.beginObject();
                while (jr.hasNext()) {
                    values.put(jr.nextName(), nextStringOrNull(jr));
                }

                if(values.get("drawing") == null){
                    values.put("drawing", "");
                }

                try {
                    String[] insert = new String[]{values.get("id"), values.get("install_id"),
                            values.get("charset"), values.get("character"), values.get("score"),
                            values.get("device_timestamp"), values.get("drawing")};
                    Log.i("nakama-sync", "Inserting remote log: " + Util.join(", ", insert));
                    DataHelper.selectRecord(sqlite, "INSERT INTO practice_log(id, install_id, charset, character, score, timestamp, drawing) VALUES(?, ?, ?, ?, ?, ?, ?)", (Object[])insert);
                } catch (SQLiteConstraintException t) {
                    Log.e("nakama", "DB error while error inserting sync log: " + Arrays.toString(values.entrySet().toArray()), t);
                }
                jr.endObject();
            }
            jr.endArray();

            jr.nextName();      // "kanji_stories" key
            jr.beginArray();
            while (jr.hasNext()) {
                Map<String, String> values = new HashMap<>();
                jr.beginObject();
                while (jr.hasNext()) {
                    values.put(jr.nextName(), nextStringOrNull(jr));
                }
/*
                Log.i("nakama", "Inserting kanji_story from record: " + Util.join(values, "=>", ", "));
                try {
                    DataHelper.selectRecord(sqlite, "UPDATE OR IGNORE kanji_stories SET story=? WHERE character = ? AND  timestamp < ?",
                            (Object[])(new String[]{ values.get("story"), values.get("character"), values.get("device_timestamp")}));

                    DataHelper.selectRecord(sqlite, "INSERT OR IGNORE INTO kanji_stories(character, story, timestamp) VALUES(?, ?, ?)",
                            new String[]{values.get("character"), values.get("story"), values.get("device_timestamp") });

                } catch (SQLiteConstraintException t) {
                    Log.e("nakama", "DB error while error inserting sync log: " + Arrays.toString(values.entrySet().toArray()), t);
                }
*/
                jr.endObject();
            }
            jr.endArray();

            jr.nextName();      // "charset_goals" key
            jr.beginArray();
            while (jr.hasNext()) {
                Map<String, String> values = new HashMap<>();
                jr.beginObject();
                while (jr.hasNext()) {
                    values.put(jr.nextName(), nextStringOrNull(jr));
                }
                Log.i("nakama", "Inserting charset goal from record: " + Util.join(values, "=>", ", "));

                try {
                    DataHelper.selectRecord(sqlite, "UPDATE OR IGNORE charset_goals SET goal=?, goal_start=? WHERE charset = ? AND timestamp < ?",
                        (Object[])(new String[]{ values.get("goal"), values.get("goal_start"), values.get("charset"), values.get("device_timestamp")}));

                    DataHelper.selectRecord(sqlite, "INSERT OR IGNORE INTO charset_goals(goal, goal_start, charset, timestamp) VALUES(?, ?, ?, ?)",
                        (Object[])(new String[]{ values.get("goal"), values.get("goal_start"), values.get("charset"), values.get("device_timestamp")}));

                    Log.i("nakama-sync", "Upserting remote story: " + Util.join(", ", values.entrySet()));
                } catch (SQLiteConstraintException t) {
                    Log.e("nakama", "DB error while error inserting sync log: " + Arrays.toString(values.entrySet().toArray()), t);
                }
                jr.endObject();
            }
            jr.endArray();

            jr.nextName();      // "character_set_edits" key
            jr.beginArray();
            while (jr.hasNext()) {
                Map<String, String> values = new HashMap<>();
                jr.beginObject();
                while (jr.hasNext()) {
                    values.put(jr.nextName(), nextStringOrNull(jr));
                }
                Log.i("nakama", "Inserting character set edit from record: " + Util.join(values, "=>", ", "));

                try {
                    CustomCharacterSetDataHelper h = new CustomCharacterSetDataHelper(context);
                    h.recordRemoteEdit( values.get("id"), values.get("charset_id"), values.get("name"), values.get("description"),
                            values.get("characters"), values.get("install_id"), values.get("timestamp"));

                    Log.i("nakama-sync", "Upserting remote story: " + Util.join(", ", values.entrySet()));
                } catch (SQLiteConstraintException t) {
                    Log.e("nakama", "DB error while error inserting sync log: " + Arrays.toString(values.entrySet().toArray()), t);
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

    public static void largeLog(String tag, String content) {
        if (content.length() > 4000 && !BuildConfig.DEBUG) {
            Log.d(tag, content.substring(0, 4000));
            largeLog(tag, content.substring(4000));
        } else {
            Log.d(tag, content);
        }
}

    private void queryToJsonArray(String name, SQLiteDatabase sqlite, String sql, String[] args, JsonWriter jw) throws IOException {
        Log.i("nakama-sync", sql + ": " + Util.join(", ", args));
        Cursor c = sqlite.rawQuery(sql, args);
        try {
            // stream over all rows since that time
            jw.name(name);
            jw.beginArray();
            while (c.moveToNext()) {
                jw.beginObject();
                for (int i = 0; i < c.getColumnCount(); i++) {
                    jw.name(c.getColumnName(i));
                    jw.value(c.getString(i));
                }
                jw.endObject();
            }
        } finally {
            c.close();
        }
        jw.endArray();
    }

    private static String nextStringOrNull(JsonReader jr) throws IOException {
        JsonToken peek = jr.peek();
        if(peek == JsonToken.NULL) {
            jr.nextNull();
            return null;
        }
        return jr.nextString();
    }
}
