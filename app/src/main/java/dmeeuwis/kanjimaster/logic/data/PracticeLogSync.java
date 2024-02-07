package dmeeuwis.kanjimaster.logic.data;

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
import java.net.ProtocolException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import dmeeuwis.kanjimaster.core.util.Util;
import dmeeuwis.kanjimaster.logic.util.JsonReader;
import dmeeuwis.kanjimaster.logic.util.JsonToken;
import dmeeuwis.kanjimaster.logic.util.JsonWriter;

public class PracticeLogSync {

    final private static String SYNC_URL = "/write-japanese/progress-sync";

    final ExternalDependencies extDeps;

    public static class ExternalDependencies {

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

    public PracticeLogSync() {
        this.extDeps = new ExternalDependencies();
    }

    public PracticeLogSync(ExternalDependencies extDeps) {
        this.extDeps = extDeps;
    }

    public void clearSync(){
        SettingsFactory.get().clearSyncSettingsDebug();
    }

    public String maxTimestamp(){
        return DataHelperFactory.get().selectString(
                "SELECT MAX(timestamp) FROM " +
                        "(SELECT MAX(timestamp) as timestamp FROM practice_log UNION " +
                        "SELECT MAX(timestamp) as timestamp FROM charset_goals UNION " +
                        "SELECT MAX(timestamp) as timestamp FROM kanji_stories UNION " +
                        "SELECT MAX(timestamp) as timestamp FROM character_set_edits)"  );
    }

    public void debugPrintLog() {
        for (Map<String, String> r : DataHelperFactory.get().selectRecords("SELECT * FROM practice_log")) {
            Log.i("nakama-sync", new JSONObject(r).toString());
        }
    }

    public String BackupToJson() throws IOException {
        long startTime = System.currentTimeMillis();

        String iid = IidFactory.get().toString();

        Settings.SyncSettings sync = SettingsFactory.get().getSyncSettings();
        Log.i("nakama-sync", "Doing sync with last-server-sync: " + sync.lastSyncServerTimestamp + "; last device sync: " + sync.lastSyncDeviceTimestamp);

        Writer netWriter = new StringWriter();
        JsonWriter jw = new JsonWriter(netWriter);

        jw.beginObject();
        jw.name("install_id").value(iid);
//      jw.name("level").value(LockCheckerInAppBillingService.getPurchaseStatus(prefs).toString());

        String jsonResponse = null;
        String jsonPost = null;

        DataHelper db = DataHelperFactory.get();
        try {
            //---------------------------------
            // creating backup as JSON
            //---------------------------------
            Log.i("nakama-sync", "Sync using: " + sync + " and iid " + iid);
            jw.name("prev_sync_timestamp").value(sync.lastSyncServerTimestamp);

            jw.name("app_version").value(SettingsFactory.get().version());

            int sendingLogs = db.queryToJsonArray("practice_logs",
                    "SELECT id, install_id, character, charset, timestamp, score, drawing " +
                            "FROM practice_log WHERE timestamp > ? AND install_id = ?",
                    new String[]{sync.lastSyncDeviceTimestamp, iid}, jw);

            int sendingGoals = db.queryToJsonArray("charset_goals",
                    "SELECT charset, timestamp, goal_start, goal FROM charset_goals WHERE timestamp > ?",
                    new String[]{sync.lastSyncDeviceTimestamp}, jw);

            int sendingStories = db.queryToJsonArray("kanji_stories",
                    "SELECT character, story, timestamp FROM kanji_stories WHERE timestamp > ?",
                    new String[]{sync.lastSyncDeviceTimestamp}, jw);

            int sendingSetEdits = db.queryToJsonArray("character_set_edits",
                    "SELECT id, charset_id, name, description, characters, timestamp as device_timestamp, deleted FROM character_set_edits WHERE timestamp > ? AND install_id = ?",
                    new String[]{sync.lastSyncDeviceTimestamp, iid}, jw);

            jw.endObject();
            jw.close();

            jsonPost = netWriter.toString();
            Log.i("nakama", "Posting JSON sync: " + jsonPost);

            //------------------------------------------------
            // backup as JSON created.
            //------------------------------------------------
        } catch (IOException e) {

        }
        return jsonPost;
    }

    public SyncCounts sync() throws IOException {
        long startTime = System.currentTimeMillis();
        String iid = IidFactory.get().toString();

        Settings.SyncSettings sync = SettingsFactory.get().getSyncSettings();
        Log.i("nakama-sync", "Doing sync with last-server-sync: " + sync.lastSyncServerTimestamp + "; last device sync: " + sync.lastSyncDeviceTimestamp);

        Writer netWriter = new StringWriter();
        JsonWriter jw = new JsonWriter(netWriter);

        jw.beginObject();
        jw.name("install_id").value(iid);
//      jw.name("level").value(LockCheckerInAppBillingService.getPurchaseStatus(prefs).toString());

        String jsonResponse = null;
        String jsonPost = null;

        DataHelper db = DataHelperFactory.get();
        try {
            //---------------------------------
            // creating backup as JSON
            //---------------------------------
            Log.i("nakama-sync", "Sync using: " + sync + " and iid " + iid);
            jw.name("prev_sync_timestamp").value(sync.lastSyncServerTimestamp);

            jw.name("app_version").value(SettingsFactory.get().version());

            int sendingLogs = db.queryToJsonArray("practice_logs",
                    "SELECT id, install_id, character, charset, timestamp, score, drawing " +
                            "FROM practice_log WHERE timestamp > ? AND install_id = ?",
                    new String[]{sync.lastSyncDeviceTimestamp, iid}, jw);

            int sendingGoals = db.queryToJsonArray("charset_goals",
                    "SELECT charset, timestamp, goal_start, goal FROM charset_goals WHERE timestamp > ?",
                    new String[]{sync.lastSyncDeviceTimestamp}, jw);

            int sendingStories = db.queryToJsonArray("kanji_stories",
                    "SELECT character, story, timestamp FROM kanji_stories WHERE timestamp > ?",
                    new String[]{sync.lastSyncDeviceTimestamp}, jw);

            int sendingSetEdits = db.queryToJsonArray("character_set_edits",
                    "SELECT id, charset_id, name, description, characters, timestamp as device_timestamp, deleted FROM character_set_edits WHERE timestamp > ? AND install_id = ?",
                    new String[]{sync.lastSyncDeviceTimestamp, iid}, jw);

            jw.endObject();
            jw.close();

            jsonPost = netWriter.toString();
            Log.i("nakama", "Posting JSON sync: " + jsonPost);

            //------------------------------------------------
            // backup as JSON created.
            //------------------------------------------------

            InputStream inStream = extDeps.sendPost(jsonPost);

            jsonResponse = Util.slurp(inStream);
            largeLog("nakama-sync", "Saw progress-sync response JSON: " + jsonResponse);


            //-----------------------------------
            // restore backup: read JSON stream
            //-----------------------------------
            Reader rin = new InputStreamReader(new ByteArrayInputStream(jsonResponse.getBytes("UTF-8")));
            JsonReader jr = new JsonReader(rin);

            jr.beginObject();
            String syncTimestampName = jr.nextName();
            String syncTimestampValue = jr.nextString();
            Log.i("nakama-sync", "Saw JSON response object sync values: " + syncTimestampName + " = " + syncTimestampValue);

            //-----------------------------------
            // restore backup: parse JSON, practice logs.
            //-----------------------------------
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
                    //Log.i("nakama-sync", "Inserting remote log: " + Util.join(", ", insert));
                    practiceLogCount++;
                    DataHelperFactory.get().selectRecord("INSERT INTO practice_log(id, install_id, charset, character, score, timestamp, drawing) VALUES(?, ?, ?, ?, ?, ?, ?)", (Object[])insert);
                } catch (Exception t) {
                    Log.e("nakama", "DB error while error inserting sync log: " + Arrays.toString(values.entrySet().toArray()), t);
                }
                jr.endObject();
            }
            jr.endArray();

            //-----------------------------------
            // restore backup: parse JSON, kanji stories.
            //-----------------------------------
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

            //-----------------------------------
            // restore backup: parse JSON, charset goals
            //-----------------------------------
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
                    DataHelperFactory.get().selectRecord("UPDATE OR IGNORE charset_goals SET goal=?, goal_start=? WHERE charset = ? AND timestamp < ?",
                        (Object[])(new String[]{ values.get("goal"), values.get("goal_start"), values.get("charset"), values.get("device_timestamp")}));

                    DataHelperFactory.get().selectRecord("INSERT OR IGNORE INTO charset_goals(goal, goal_start, charset, timestamp) VALUES(?, ?, ?, ?)",
                        (Object[])(new String[]{ values.get("goal"), values.get("goal_start"), values.get("charset"), values.get("device_timestamp")}));
                    charsetGoalsCount++;

                    Log.i("nakama-sync", "Upserting remote story: " + Util.join(", ", values.entrySet()));
                } catch (Exception t) {
                    Log.e("nakama", "DB error while error inserting sync log: " + Arrays.toString(values.entrySet().toArray()), t);
                }
                jr.endObject();
            }
            jr.endArray();

            //-----------------------------------
            // restore backup: parse JSON, custom charsets.
            //-----------------------------------
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
                    Log.i("nakama-sync", "Inserting character set edit from record: " + Util.join(values, "=>", ", "));

                    try {
                        CustomCharacterSetDataHelper h = new CustomCharacterSetDataHelper();
                        h.recordRemoteEdit(values.get("id"), values.get("charset_id"), values.get("name"), values.get("description"),
                                values.get("characters"), values.get("install_id"), values.get("timestamp"), Boolean.parseBoolean(values.get("deleted")));
                        charsetEditCount++;

                        Log.i("nakama-sync", "Upserting remote story: " + Util.join(", ", values.entrySet()));
                    } catch (Exception t) {
                        Log.e("nakama-sync", "DB error while error inserting sync log: " + Arrays.toString(values.entrySet().toArray()), t);
                    }
                    jr.endObject();
                }
                jr.endArray();
            }

            jr.endObject();

            jr.close();
            rin.close();
            inStream.close();
            //-----------------------------------
            // restore backup completed.
            //-----------------------------------


            //-----------------------------------
            // Set last backup sync timestamp in settings.
            // If we're force-updating, no need to keep this.
            //-----------------------------------
            Settings.SyncSettings set = new Settings.SyncSettings(syncTimestampValue, maxTimestamp());
            SettingsFactory.get().setSyncSettings(set);

            Log.i("nakama-sync", "Sync complete! Set lastSyncDeviceTimestamp to " + set.lastSyncDeviceTimestamp + " and lastSyncServerTimestamp to " + set.lastSyncServerTimestamp);

            return new SyncCounts(practiceLogCount, kanjiStoriesCount, charsetGoalsCount, charsetEditCount,
                                    sendingLogs,                 sendingStories,        sendingGoals,         sendingSetEdits);

        } catch(IOException t) {
            Log.e("nakama-sync", "Network error during sync. Ignoring, will try again next sync.");
            return new SyncCounts(-1, -1, -1, -1, -1, -1, -1, -1);
        } catch(Throwable t) {
            StringBuilder message = new StringBuilder();
            message.append("Error when parsing sync; request was: " + (jsonPost == null ? "<null>" : jsonPost));
            message.append("response was: " + (jsonResponse == null ? "<null>" : jsonResponse));
            message.append("; time in sync was " + (System.currentTimeMillis() - startTime) + "ms");
            throw new RuntimeException(message.toString(), t);
        }
    }

    public static class SyncCounts {
        public final int practiceLogsReceived;
        public final int storiesReceived;
        public final int charsetGoalsReceived;
        public final int charsetEditsReceived;

        public final int practiceLogsSent;
        public final int storiesSent;
        public final int charsetGoalsSent;
        public final int charsetEditsSent;

        public SyncCounts(int practiceLogsReceived, int storiesReceived, int charsetGoalsReceived, int charsetEditsReceived, int practiceLogsSent, int storiesSent, int charsetGoalsSent, int charsetEditsSent) {
            this.practiceLogsReceived = practiceLogsReceived;
            this.storiesReceived = storiesReceived;
            this.charsetGoalsReceived = charsetGoalsReceived;
            this.charsetEditsReceived = charsetEditsReceived;
            this.practiceLogsSent = practiceLogsSent;
            this.storiesSent = storiesSent;
            this.charsetGoalsSent = charsetGoalsSent;
            this.charsetEditsSent = charsetEditsSent;
        }
    }

    public static void largeLog(String tag, String content) {
        if (content.length() > 4000 && SettingsFactory.get().debug()) {
            Log.d(tag, content.substring(0, 4000));
        } else {
            Log.d(tag, content);
        }
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
