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
import java.net.ProtocolException;
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
    final private static String SYNC_URL = "/write-japanese/progress-sync";

    final ExternalDependencies extDeps;
    final Context context;

    public static class ExternalDependencies {
        private final Context context;

        public ExternalDependencies(Context c){
            context = c;
        }

        public InputStream sendPost(String jsonPost) throws Exception {
            URL syncUrl = HostFinder.formatURL(SYNC_URL);

            HttpURLConnection urlConnection = (HttpURLConnection) syncUrl.openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            try {
                urlConnection.setRequestMethod("POST");
            } catch(ProtocolException e){
                throw new RuntimeException(e);  // really?
            }
            urlConnection.setRequestProperty("Content-Type", "application/json");
            OutputStream out = urlConnection.getOutputStream();
            try {
                out.write(jsonPost.getBytes("UTF-8"));
            } finally {
                out.close();
            }

            // stream over standardSets rows in from POST response
            Log.i("nakama", "Received response to JSON sync at " + syncUrl + ": " + urlConnection.getResponseMessage() + urlConnection.getResponseMessage());
            return urlConnection.getInputStream();
        }


    }

    public PracticeLogSync(Context c) {
        this.extDeps = new ExternalDependencies(c);
        this.context = c;
    }

    public PracticeLogSync(ExternalDependencies extDeps, Context c) {
        this.extDeps = extDeps;
        this.context = c;
    }

    public void clearSync(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(extDeps.context);
        SharedPreferences.Editor e = prefs.edit();
        e.putString(SERVER_SYNC_PREFS_KEY, "2000-01-01 00:00:00 +00");
        e.putString(DEVICE_SYNC_PREFS_KEY, "0");
        e.apply();
    }

    public String maxTimestamp(WriteJapaneseOpenHelper db){
        SQLiteDatabase d = db.getReadableDatabase();
        try {
            return DataHelper.selectString(d,
                    "SELECT MAX(timestamp) FROM " +
                            "(SELECT MAX(timestamp) as timestamp FROM practice_log UNION " +
                            "SELECT MAX(timestamp) as timestamp FROM charset_goals UNION " +
                            "SELECT MAX(timestamp) as timestamp FROM kanji_stories UNION " +
                            "SELECT MAX(timestamp) as timestamp FROM character_set_edits)"  );
        } finally {
            d.close();
        }
    }

    public void debugPrintLog(){
        WriteJapaneseOpenHelper db = new WriteJapaneseOpenHelper(context);
        SQLiteDatabase sqlite = db.getReadableDatabase();

        try {
            for(Map<String, String> r:DataHelper.selectRecords(sqlite, "SELECT * FROM practice_log")){
                Log.i("nakama-sync", new JSONObject(r).toString());
            }
        } finally {
            sqlite.close();
        }
    }

    public Map<String, Integer> sync() throws IOException {
        long startTime = System.currentTimeMillis();
        String iid = Iid.get(extDeps.context).toString();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(extDeps.context);
        String lastSyncServerTimestamp1 = prefs.getString(SERVER_SYNC_PREFS_KEY, "2000-01-01 00:00:00 +00");
        String lastSyncDeviceTimestamp1 = prefs.getString(DEVICE_SYNC_PREFS_KEY, "0");
        Log.i("nakama-sync", "Doing sync with last-server-sync: " + lastSyncServerTimestamp1 + "; last device sync: " + lastSyncDeviceTimestamp1);

        Map<String, Object> syncDetails = Util.makeObjectMap(SERVER_SYNC_PREFS_KEY, lastSyncServerTimestamp1, DEVICE_SYNC_PREFS_KEY, lastSyncDeviceTimestamp1);
        String lastSyncServerTimestamp = (String)syncDetails.get(SERVER_SYNC_PREFS_KEY);
        String lastSyncDeviceTimestamp = (String)syncDetails.get(DEVICE_SYNC_PREFS_KEY);
        Log.i("nakama-sync", "Doing sync with last-server-sync: " + lastSyncServerTimestamp + "; last device sync: " + lastSyncDeviceTimestamp);

        Writer netWriter = new StringWriter();
        JsonWriter jw = new JsonWriter(netWriter);

        jw.beginObject();
        jw.name("install_id").value(iid);
        jw.name("level").value(LockCheckerIabHelper.getPurchaseStatus(prefs).toString());

        String jsonResponse = null;
        String jsonPost = null;

        WriteJapaneseOpenHelper db = new WriteJapaneseOpenHelper(context);
        SQLiteDatabase sqlite = db.getWritableDatabase();
        try {
            jw.name("prev_sync_timestamp").value(lastSyncServerTimestamp);

            jw.name("app_version").value(BuildConfig.VERSION_CODE);

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

            jsonPost = netWriter.toString();
            Log.i("nakama", "Posting JSON sync: " + jsonPost);

            InputStream inStream = extDeps.sendPost(jsonPost);

            jsonResponse = Util.slurp(inStream);
            largeLog("nakama-sync", "Saw progress-sync response JSON: " + jsonResponse);

            Reader rin = new InputStreamReader(new ByteArrayInputStream(jsonResponse.getBytes("UTF-8")));
            JsonReader jr = new JsonReader(rin);

            jr.beginObject();
            String syncTimestampName = jr.nextName();
            String syncTimestampValue = jr.nextString();
            Log.i("nakama-sync", "Saw JSON response object sync values: " + syncTimestampName + " = " + syncTimestampValue);

            int practiceLogCount = 0;
            String n = jr.nextName();      // "practice_logs" key
            if(!"practice_logs".equals(n)){
                throw new RuntimeException("Expected 'practice_logs' but saw " + n);
            }
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
                    practiceLogCount++;
                    DataHelper.selectRecord(sqlite, "INSERT INTO practice_log(id, install_id, charset, character, score, timestamp, drawing) VALUES(?, ?, ?, ?, ?, ?, ?)", (Object[])insert);
                } catch (SQLiteConstraintException t) {
                    Log.e("nakama", "DB error while error inserting sync log: " + Arrays.toString(values.entrySet().toArray()), t);
                }
                jr.endObject();
            }
            jr.endArray();

            int kanjiStoriesCount = 0;
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

            int charsetGoalsCount = 0;
            jr.nextName();      // "charset_goals" key
            jr.beginArray();
            while(jr.hasNext()) {
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
                    charsetGoalsCount++;

                    Log.i("nakama-sync", "Upserting remote story: " + Util.join(", ", values.entrySet()));
                } catch (SQLiteConstraintException t) {
                    Log.e("nakama", "DB error while error inserting sync log: " + Arrays.toString(values.entrySet().toArray()), t);
                }
                jr.endObject();
            }
            jr.endArray();

            int charsetEditCount = 0;
            if(jr.hasNext()) {
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
                        h.recordRemoteEdit(values.get("id"), values.get("charset_id"), values.get("name"), values.get("description"),
                                values.get("characters"), values.get("install_id"), values.get("timestamp"), Boolean.parseBoolean(values.get("deleted")));
                        charsetEditCount++;

                        Log.i("nakama-sync", "Upserting remote story: " + Util.join(", ", values.entrySet()));
                    } catch (SQLiteConstraintException t) {
                        Log.e("nakama", "DB error while error inserting sync log: " + Arrays.toString(values.entrySet().toArray()), t);
                    }
                    jr.endObject();
                }
                jr.endArray();
            }

            jr.endObject();

            jr.close();
            rin.close();
            inStream.close();

            SharedPreferences.Editor ed = prefs.edit();
            ed.putString(DEVICE_SYNC_PREFS_KEY, maxTimestamp(db));
            ed.putString(SERVER_SYNC_PREFS_KEY, syncTimestampValue);
            ed.apply();

            Log.i("nakama-sync", "Sync complete!");

            return Util.makeCountMap("practiceLogs", practiceLogCount, "charsetGoals", charsetGoalsCount, "charsetEdits", charsetEditCount);

        } catch(Throwable t) {
            StringBuilder message = new StringBuilder();
            message.append("Error when parsing sync; request was: " + (jsonPost == null ? "<null>" : jsonPost));
            message.append("response was: " + (jsonResponse == null ? "<null>" : jsonResponse));
            message.append("; time in sync was " + (System.currentTimeMillis() - startTime) + "ms");
            throw new RuntimeException(message.toString(), t);
        } finally {
            sqlite.close();
            db.close();
        }
    }

    public static void largeLog(String tag, String content) {
        if (content.length() > 4000 && !BuildConfig.DEBUG) {
            Log.d(tag, content.substring(0, 4000));
        } else {
            Log.d(tag, content);
        }
    }

    private void queryToJsonArray(String name, SQLiteDatabase sqlite, String sql, String[] args, JsonWriter jw) throws IOException {
        Log.i("nakama-sync", sql + ": " + Util.join(", ", args));
        Cursor c = sqlite.rawQuery(sql, args);
        try {
            // stream over standardSets rows since that time
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
