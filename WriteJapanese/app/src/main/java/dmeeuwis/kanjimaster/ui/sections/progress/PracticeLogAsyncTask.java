package dmeeuwis.kanjimaster.ui.sections.progress;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dmeeuwis.kanjimaster.logic.data.DataHelper;
import dmeeuwis.kanjimaster.logic.data.UncaughtExceptionLogger;
import dmeeuwis.kanjimaster.logic.drawing.PointDrawing;
import dmeeuwis.kanjimaster.ui.data.DataHelperAndroid;

class PracticeLogAsyncTask extends AsyncTask<String, String, List<PracticeLogAsyncTask.PracticeLog>> {

    String setName;
    Context ctx;
    PracticeLogAsyncCallback callback;

    interface PracticeLogAsyncCallback {
        public void onCompletion(List<PracticeLog> logs);
    }

    PracticeLogAsyncTask(String setName, Context ctx, PracticeLogAsyncCallback callback){
        this.setName = setName;
        this.ctx = ctx;
        this.callback = callback;
    }

    public static class PracticeLog {
        String character;
        String date;
        boolean correct;
        PointDrawing drawing;

        public PracticeLog(String character, String date, String score, PointDrawing drawing){
           this.character = character;
           this.date = date;
           this.correct = score.equals("100");
           this.drawing = drawing;
        }
    }

    @Override
    protected List<PracticeLog> doInBackground(String... strings) {
        DataHelper dw = new DataHelperAndroid(ctx);
        try {
            List<Map<String, String>> rows = dw.selectRecords(
                    "SELECT character, timestamp, score, drawing FROM practice_log WHERE charset = ? ORDER BY timestamp DESC LIMIT 1000",
                    setName);

            List<PracticeLog> out = new ArrayList<>(rows.size());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            DateTimeFormatter display = DateTimeFormatter.ofPattern("yyyy-MM-dd h:mm a");
            for(Map<String, String> r: rows){
                try {
                    String json = r.get("drawing");
                    PointDrawing pd = null;
                    if(json != null && !json.isEmpty()) {
                        if(json.startsWith("\"")){
                            json = json.replace("\\\"", "\"");
                            json = json.substring(1, json.length());
                        }
                        try {
                            pd = PointDrawing.deserialize(json);
                        } catch (Throwable t) {
                            Log.e("nakama", "Error parsing practice log drawing: " + json, t);
                        }
                    }

                    String date = "";
                    try {
                        date = LocalDateTime.parse((String)r.get("timestamp"), formatter)
                                .atOffset(ZoneOffset.UTC)
                                .atZoneSameInstant(ZoneId.systemDefault())
                                .format(display);
                    } catch (Throwable t){
                        Log.e("nakama", "Error parsing timestamp", t);
                    }

                    PracticeLog p = new PracticeLog((String) r.get("character"), date, (String) r.get("score"), pd);
                    out.add(p);
                } catch(Throwable t){
                    Log.e("nakama", "Error parsing practice log: " + t.getMessage(), t);
                }
            }
            Log.i("nakama", "Loaded data for " + rows.size() + " practice logs.");
            return out;
        } catch (Throwable t){
            UncaughtExceptionLogger.backgroundLogError("Error retrieving practice logs.", t);
            return new ArrayList<>();
        }
    }

    @Override
    protected void onPostExecute(List<PracticeLog> rows){
        super.onPostExecute(rows);
        callback.onCompletion(rows);
    }
}
