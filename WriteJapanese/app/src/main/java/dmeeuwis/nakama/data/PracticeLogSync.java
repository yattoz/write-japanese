package dmeeuwis.nakama.data;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import dmeeuwis.nakama.primary.Iid;

public class PracticeLogSync {
    final Activity activity;

    public PracticeLogSync(Activity activity){
        this.activity = activity;
    }

    public void sync() throws IOException {
        WriteJapaneseOpenHelper db = new WriteJapaneseOpenHelper(this.activity.getApplicationContext());
        SQLiteDatabase sqlite = db.getReadableDatabase();
        URL syncUrl;
        String iid = Iid.get(activity.getApplication()).toString();
        try {
            syncUrl = new URL("http://dmeeuwis.com/practice_log_sync");
        } catch(MalformedURLException e){
            throw new RuntimeException(e);
        }

        HttpURLConnection urlConnection = (HttpURLConnection)syncUrl.openConnection();
        urlConnection.setDoOutput(true);
        urlConnection.setDoInput(true);
        urlConnection.setRequestMethod("POST");
        Writer netWriter = new OutputStreamWriter(urlConnection.getOutputStream());
        JsonWriter jw = new JsonWriter(netWriter);

        jw.name("install_id");
        jw.value(iid);

        sqlite.beginTransaction();
        try {

            // find last sync guid and that row's time
            Map<String, String> lastSync =
                DataHelper.selectRecord(sqlite, "SELECT id, device_timestamp, sync_timestamp FROM network_syncs ORDER BY device_timestamp DESC LIMIT 1");
            jw.name("prev-sync-timestamp");
            jw.value(lastSync.get("sync_timestamp"));

            String lastSyncTime = "2000-01-01 00:00:00";
            if(lastSync != null){
               lastSyncTime = lastSync.get("sync_timestamp");
            }

            Cursor c = sqlite.rawQuery("SELECT * FROM practice_logs WHERE device_timestamp > ? AND install_id = ?", new String[]{lastSyncTime, iid});
            try {
                // stream over all rows since that time
                jw.name("practice_logs");
                jw.beginArray();
                while (!c.isAfterLast()) {
                    jw.beginObject();
                    for (int i = 0; i < c.getColumnCount(); i++) {
                        jw.name(c.getColumnName(i));
                        jw.value(c.getString(i));
                    }
                    jw.endObject();
                    c.moveToNext();
                }
            } finally {
                c.close();
            }
            jw.endArray();
            netWriter.close();
            sqlite.endTransaction();

            // stream over all rows in from POST response
            Reader rin = new InputStreamReader(urlConnection.getInputStream());
            JsonReader jr = new JsonReader(rin);

            jr.beginObject();
            String syncTimestampName = jr.nextName();
            String syncTimestampValue = jr.nextString();
            DataHelper.selectRecord(sqlite, "INSERT INTO network_syncs(id, device_timestamp, sync_timestamp) VALUES(?, ?, ?)",
                    UUID.randomUUID().toString(), new GregorianCalendar().getTime(), syncTimestampValue);

            Map<String, String> values = new HashMap<>();
            jr.beginArray();
            while(jr.hasNext()){
                jr.beginObject();
                while (jr.hasNext()) {
                    values.put(jr.nextName(), jr.nextString());
                    try {
                        String[] insert = new String[]{values.get("id"), values.get("install_id"),
                                values.get("charset"), values.get("character"), values.get("score"),
                                values.get("timestamp")};
                        DataHelper.selectRecord(sqlite, "INSERT INTO practice_logs(install_id, id, charset, character, score, timestamp", insert);
                    } catch(RuntimeException e){
                        if(e.getCause() instanceof  SQLException) {
                            Log.e("nakama", "DB error while error inserting sync log: " + Arrays.toString(values.entrySet().toArray()));
                        } else {
                            throw e;
                        }
                    }
                }
                jr.endObject();
            }
            jr.endArray();

        } finally {
            db.close();
        }
    }
}