package dmeeuwis.nakama.data;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import dmeeuwis.nakama.primary.Iid;

public class PracticeLogSync {
    final Activity activity;
    final String authcode;

    public PracticeLogSync(Activity activity, String authcode){
        this.activity = activity;
        this.authcode = authcode;
    }

    public void sync() throws IOException {
        WriteJapaneseOpenHelper db = new WriteJapaneseOpenHelper(this.activity.getApplicationContext());
        SQLiteDatabase sqlite = db.getReadableDatabase();
        URL syncUrl;
        try {
            syncUrl = new URL("https://dmeeuwis.com/practice_log_sync");
        } catch(MalformedURLException e){
            throw new RuntimeException(e);
        }

        HttpURLConnection urlConnection = (HttpURLConnection)syncUrl.openConnection();
        urlConnection.setDoOutput(true);
        urlConnection.setDoInput(true);
        urlConnection.setRequestMethod("POST");
        Writer netWriter = new OutputStreamWriter(urlConnection.getOutputStream());
        JsonWriter jw = new JsonWriter(netWriter);

        jw.name("iid");
        jw.value(Iid.get(activity.getApplication()).toString());

        jw.name("authcode");
        jw.value(authcode);

        try {
            sqlite.beginTransaction();

            // find last sync guid and that row's time
            Map<String, String> lastSync =
                DataHelper.selectRecord(sqlite, "SELECT practice_log_id, device_timestamp, server_timestamp FROM network_syncs ORDER BY device_timestamp DESC LIMIT 1");

            Cursor c = sqlite.rawQuery("SELECT * FROM practice_logs WHERE device_timestamp > ?", deviceTimestamp);

            // stream over all rows since that time
            jw.name("practice_logs");
            jw.beginArray();
            while(!c.isAfterLast()){
                Map<String, Object> record = new HashMap<>();
                jw.beginObject();
                for(int i = 0; i < c.getColumnCount(); i++){
                    jw.name(c.getColumnName(i));
                    jw.value(c.getString(i));
                }
                jw.endObject();
                c.moveToNext();
            }
            jw.endArray();
            netWriter.close();
            sqlite.endTransaction();

            // stream over all rows in from POST response
            Reader rin = new InputStreamReader(urlConnection.getInputStream());
            JsonReader jr = new JsonReader(rin);
            jr.beginArray();
            while(jr.hasNext()){
                jr.beginObject();

            }


                // insert into db
        } finally {
            db.close();
        }
    }
}
