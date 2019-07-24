package dmeeuwis.kanjimaster.ui.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.format.DateTimeParseException;

import java.util.HashMap;
import java.util.Map;

import dmeeuwis.kanjimaster.BuildConfig;
import dmeeuwis.kanjimaster.logic.data.CharacterProgressDataHelper;
import dmeeuwis.kanjimaster.logic.data.DataHelper;
import dmeeuwis.kanjimaster.logic.data.ProgressTracker;

public class ProcessLogRowAndroid implements DataHelper.ProcessRow {
    private Context context;

    public ProcessLogRowAndroid(Context context) {
        this.context = context;
    }

    public int applyToResults(String sql, ProgressTracker pt, Object... params){
        WriteJapaneseOpenHelper woh = new WriteJapaneseOpenHelper(context);
        SQLiteDatabase db = woh.getReadableDatabase();
        String[] sparams = new DataHelperAndroid(context).asStringArray(params);

        Cursor c = db.rawQuery(sql,sparams);
        int count = 0;
        try {
            int columnCount = c.getColumnCount();
            while(c.moveToNext()){
                count++;
                Map<String, String> m = new HashMap<String, String>();
                for(int i = 0; i < columnCount; i++){
                    String colName = c.getColumnName(i);
                    String rowColValue = c.getString(i);
                    m.put(colName, rowColValue);
                }
                process(m, pt);
            }
        } finally {
            if(c != null) c.close();
        }
        return count;
    }

    @Override
    public void process(Map<String, String> r, ProgressTracker pt) {
        Character character = r.get("character").charAt(0);
        String set = r.get("charset");
        String timestampStr = r.get("timestamp");
        Integer score = Integer.parseInt(r.get("score"));
        String iid = r.get("install_id");
        LocalDateTime t;

        try {
            t = LocalDateTime.parse(timestampStr, CharacterProgressDataHelper.formatter);
        } catch(DateTimeParseException e){
            try {
                t = LocalDateTime.parse(timestampStr.replace(' ', 'T'), CharacterProgressDataHelper.formatter);
            } catch (DateTimeParseException x) {
                UncaughtExceptionLogger.backgroundLogError("Error caught parsing timestamp on practice log: " + timestampStr, x, context);
                return;
            }
        }

        if(pt.reject(character)){
            return;
        }

        pt.noteTimestamp(character, t, iid);

        if(BuildConfig.DEBUG){
            //Log.d("nakama-progress", "Processing practice log: " + character + " " + set + " " + timestampStr + " " + score + " on set tracker " + pt.setId);
        }

        if (character.equals('R')) {
            // indicates reset progress for standardSets characters
            pt.progressReset(set);
            //Log.d("nakama-progress", "Loaded PROGRESS RESET for set " + set);

        } else if (character.equals('S')) {
            // indicates reset progress for srs sets (maybe only on first srs install?)
            pt.srsReset(set);
            //Log.d("nakama-progress", "Loaded SRS RESET for set " + set);


        } else {
            if (score == 100) {
                pt.markSuccess(character, t);
                //Log.d("nakama-progress", "Loaded PASS result for " + character + " in set " + set + "; currently at " + pt.debugPeekCharacterScore(character));

            } else if (score == ProgressTracker.Progress.FAILED.forceResetCode) {
                // indicates reset progress to failed for a single character
                pt.resetTo(character, ProgressTracker.Progress.FAILED);

            } else if (score == ProgressTracker.Progress.REVIEWING.forceResetCode){
                // indicates reset progress to timed review for a single character
                pt.resetTo(character, ProgressTracker.Progress.REVIEWING);

            } else if (score == ProgressTracker.Progress.TIMED_REVIEW.forceResetCode){
                // indicates reset progress to timed review for a single character
                pt.resetTo(character, ProgressTracker.Progress.TIMED_REVIEW);

            } else if (score == ProgressTracker.Progress.PASSED.forceResetCode){
                // indicates reset progress to passed for a single character
                pt.resetTo(character, ProgressTracker.Progress.PASSED);


            } else {
                pt.markFailure(character);
                //Log.d("nakama-progress", "Loaded FAIL result for " + character + " in set " + set + "; currently at " + pt.debugPeekCharacterScore(character));
            }
        }
    }
}
