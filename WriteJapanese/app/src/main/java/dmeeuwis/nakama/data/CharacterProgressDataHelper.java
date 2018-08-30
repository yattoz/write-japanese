package dmeeuwis.nakama.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Pair;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.DateTimeParseException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import dmeeuwis.kanjimaster.BuildConfig;
import dmeeuwis.nakama.kanjidraw.PointDrawing;
import dmeeuwis.nakama.primary.ProgressSettingsDialog;

public class CharacterProgressDataHelper {
    public final static int DEFAULT_INTRO_INCORRECT = 5;

    public final static int DEFAULT_INTRO_REVIEWING = 10;
    public final static int DEFAULT_ADV_INCORRECT = 1;
    public final static int DEFAULT_ADV_REVIEWING = 2;
    public final static int DEFAULT_CHAR_COOLDOWN = 4;
    public final static boolean DEFAULT_SKIP_SRS_ON_FIRST_CORRECT = true;

    private final Context context;
    private final UUID iid;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    String lastRowId = null;

    public CharacterProgressDataHelper(Context c, UUID iid){
        this.context = c;
        this.iid = iid;
    }

    public void  cachePracticeRecord(String setId, String practiceRecord, String srsQueue, String oldestLog){
        WriteJapaneseOpenHelper db = new WriteJapaneseOpenHelper(this.context);
        try {
            //Log.i("nakama", "Caching progress for " + setId + ": " + practiceRecord + "; " + srsQueue);
            db.getWritableDatabase().execSQL("INSERT OR REPLACE INTO practice_record_cache(set_id, practice_record, srs_queue, last_log_by_device) VALUES(?, ?, ?, ?)",
                    new String[]{ setId, practiceRecord, srsQueue, oldestLog });
        } finally {
            db.close();
        }
    }

    public void srsReset(String charSet){
        WriteJapaneseOpenHelper db = new WriteJapaneseOpenHelper(this.context);
        try {
            db.getWritableDatabase().execSQL("INSERT INTO practice_log(id, install_id, character, charset, score) VALUES(?, ?, ?, ?, ?)",
                    new String[]{ UUID.randomUUID().toString(), iid.toString(), "S", charSet, "0" });
        } finally {
            db.close();
        }
    }

    public void clearProgress(String charSet){
        WriteJapaneseOpenHelper db = new WriteJapaneseOpenHelper(this.context);
        try {
            db.getWritableDatabase().execSQL("INSERT INTO practice_log(id, install_id, character, charset, score) VALUES(?, ?, ?, ?, ?)",
                    new String[]{ UUID.randomUUID().toString(), iid.toString(), "R", charSet, "0" });
        } finally {
            db.close();
        }
    }
    
    public String recordPractice(String charset, String character, PointDrawing d, int score){
        String serialized = null;
        if(d != null){
            serialized = d.serialize();
        }
        String rowId = UUID.randomUUID().toString();
        WriteJapaneseOpenHelper db = new WriteJapaneseOpenHelper(this.context);
        try {
            db.getWritableDatabase().execSQL("INSERT INTO practice_log(id, install_id, character, charset, score, drawing) VALUES(?, ?, ?, ?, ?, ?)",
                    new String[]{rowId, iid.toString(), character, charset, Integer.toString(score), serialized });
        } finally {
            db.close();
        }
        lastRowId = rowId;
        return rowId;
    }

    public void recordGoals(String charset, GregorianCalendar goalStart, GregorianCalendar goal) {
        SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd");

        WriteJapaneseOpenHelper db = new WriteJapaneseOpenHelper(this.context);
        try {
           if(goal== null){
               db.getWritableDatabase().execSQL("DELETE FROM charset_goals WHERE charset = ?",
                       new String[]{charset});
                return;
           }

            String goalStartStr = sd.format(goalStart.getTime());
            String goalStr = sd.format(goal.getTime());
            Pair<GregorianCalendar, GregorianCalendar> goals = getExistingGoals(charset);
            if (goals == null) {
                db.getWritableDatabase().execSQL("INSERT INTO charset_goals(charset, goal_start, goal, timestamp) VALUES(?, ?, ?, current_timestamp)",
                        new String[]{charset, goalStartStr, goalStr});
            } else {
                db.getWritableDatabase().execSQL("UPDATE charset_goals SET goal_start = ?, goal = ?, timestamp=current_timestamp WHERE charset = ?",
                        new String[]{goalStartStr, goalStr, charset});
            }
        } finally {
            db.close();
        }
    }

    public Pair<GregorianCalendar, GregorianCalendar> getExistingGoals(String charset){
        WriteJapaneseOpenHelper db = new WriteJapaneseOpenHelper(this.context);
        try {
            Map<String, String> rec = DataHelper.selectRecord(db.getReadableDatabase(),
                    "SELECT goal_start, goal FROM charset_goals WHERE charset = ? ORDER BY timestamp DESC LIMIT 1", charset);
            if(rec == null) { return null; }
            return Pair.create(parseCalendarString(rec.get("goal_start")), parseCalendarString(rec.get("goal")));
        } finally {
            db.close();
        }
    }


    public void loadProgressTrackerFromDB(final List<ProgressTracker> allPts, ProgressCacheFlag cacheFlag){
        loadProgressTrackerFromDB(allPts, false, cacheFlag);
    }

    public void resumeProgressTrackerFromDB(List<ProgressTracker> allPts) {
        loadProgressTrackerFromDB(allPts, true, ProgressCacheFlag.USE_RAW_LOGS);
    }

    public enum ProgressCacheFlag { USE_CACHE, USE_RAW_LOGS }

    private void loadProgressTrackerFromDB(final List<ProgressTracker> allPtsOrig, boolean resuming, ProgressCacheFlag useCache ){
        long start = System.currentTimeMillis();

        List<ProgressTracker> allPts = new ArrayList<>(allPtsOrig);

        WriteJapaneseOpenHelper db = new WriteJapaneseOpenHelper(this.context);

        long startResume = System.currentTimeMillis();
        if(!resuming && useCache == ProgressCacheFlag.USE_CACHE){
            // check timestamp of log vs json

            Map<String, Map<String, String>> caches = DataHelper.selectRecordsIndexedByFirst(db.getReadableDatabase(),
                "SELECT set_id, practice_record, srs_queue, last_log_by_device FROM practice_record_cache",
                "set_id");

            for (int i = allPts.size() - 1; i >= 0; i--) {
                ProgressTracker t = allPts.get(i);
                Map<String, String> cache = caches.get(t.setId);

                if(cache != null) {
                    String srsCache = cache.get("srs_queue");
                    String recordCache = cache.get("practice_record");
                    String lastLogCache = cache.get("last_log_by_device");
                    try {
                        long deserializeStart = System.currentTimeMillis();
                        t.deserializeIn(srsCache, recordCache, lastLogCache);
                        Log.d("nakama-progress", "Time to deserialize " + t.setId + ": " + (System.currentTimeMillis() - deserializeStart) + "ms");
                        allPts.remove(i);

                        resuming = true;
                    } catch (Throwable x) {
                        Log.i("nakama-progress", "Disregarding invalid cache for " + t.setId, x);
                        UncaughtExceptionLogger.backgroundLogError(
                                "Disregarding invalid cache for  " + t.setId + "; SRS cache: " + srsCache +
                                        "; log cache: " + recordCache + "; lastLogCache: " + lastLogCache,
                                new RuntimeException());
                    }
                }
            }
        }
        Log.d("nakama-progress", "Time to do resume block: " + (System.currentTimeMillis() - startResume));

        long processStart = System.currentTimeMillis();
        int allCount = 0;
        try {
            ProcessLogRow plr = new ProcessLogRow(allPts);
            if(resuming){

                for(ProgressTracker pt: allPts){
                    for(Map.Entry<String, LocalDateTime> e: pt.oldestLogTimestampByDevice.entrySet()){

                        long queryStart = System.currentTimeMillis();
                        String timestamp = e.getValue() == null ? "1900-01-01 00:00:00" : e.getValue().toString().replace('T', ' ');
                        int rowCount = DataHelper.applyToResults(plr, db.getReadableDatabase(),
                                "SELECT character, charset, score, timestamp, install_id FROM practice_log WHERE install_id = ? AND timestamp > datetime(?) and charset = ?",
                                e.getKey(), timestamp, pt.setId);

                        if(rowCount > 0) {
                            Log.d("nakama-progress", "Query took: " + (System.currentTimeMillis() - queryStart) + "ms");
                            Log.d("nakama-progress",
                                    "SELECT character, charset, score, timestamp, install_id FROM practice_log WHERE install_id = '" + e.getKey() +
                                            "' AND timestamp > datetime('"+ timestamp + "') and charset = '" + pt.setId + "'");
                            Log.d("nakama-progress", "Processed " + rowCount + " rows on " + pt.setId + " after resuming from " + e.getValue() + " on install " + e.getKey());
                        }
                        allCount += rowCount;
                    }
                }

            } else {
                allCount = DataHelper.applyToResults(plr, db.getReadableDatabase(), "SELECT character, charset, score, timestamp, install_id FROM practice_log");
            }
        } finally {
            db.close();
        }
        Log.d("nakama-progress", "Time to do process block: " + (System.currentTimeMillis() - processStart));

        long startup = System.currentTimeMillis() - start;
        Log.i("nakama-progress", "Time to load progress tracker: " + startup + "ms; counted " + allCount + " records.");
        if(startup > 2500){
            UncaughtExceptionLogger.backgroundLogError("Long startup detected: " + startup + "ms to load " + allCount + " practice logs.", new RuntimeException(), context);
        }
    }

    private static GregorianCalendar parseCalendarString(String in){
        if(in == null) { return null; }
        String[] parts = in.split("-");
        try {
            return new GregorianCalendar(Integer.valueOf(parts[0]), Integer.valueOf(parts[1]) - 1, Integer.valueOf(parts[2]));
        } catch(NumberFormatException e){
            Log.d("nakama", "ERROR: caught parse error on parseCalendarString, input: " + in, e);
            return null;
        }
    }

    public void resetTo(String charsetId, String character, ProgressTracker.Progress progress) {
        recordPractice(charsetId, character, null, progress.forceResetCode);
    }

    public void overRideLast() {
        if(lastRowId != null) {
            WriteJapaneseOpenHelper db = new WriteJapaneseOpenHelper(this.context);
            try {
                DataHelper.selectRecord(db.getReadableDatabase(),
                        "UPDATE practice_log SET score = 100 WHERE id = ?", lastRowId);
            } finally {
                db.close();
            }
        }
    }

    public void clearPracticeRecord() {
        WriteJapaneseOpenHelper db = new WriteJapaneseOpenHelper(this.context);
        try {
            db.getWritableDatabase().execSQL("DELETE FROM practice_record_cache");
        } finally {
            db.close();
        }
    }


    public static class ProgressionSettings {
        public final int introIncorrect, introReviewing, advanceIncorrect, advanceReviewing, characterCooldown;
        public final boolean skipSRSOnFirstTimeCorrect;

        public ProgressionSettings(int introIncorrect, int introReviewing, int advanceIncorrect, int advanceReviewing, int characterCooldown, boolean skipSRSOnFirstTimeCorrect) {
            this.introIncorrect = introIncorrect;
            this.introReviewing = introReviewing;
            this.advanceIncorrect = advanceIncorrect;
            this.advanceReviewing = advanceReviewing;
            this.characterCooldown = characterCooldown;
            this.skipSRSOnFirstTimeCorrect = skipSRSOnFirstTimeCorrect;
        }
    }

    public ProgressionSettings getProgressionSettings(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return new ProgressionSettings(
            prefs.getInt(ProgressSettingsDialog.SHARED_PREFS_KEY_INTRO_INCORRECT, DEFAULT_INTRO_INCORRECT),
            prefs.getInt(ProgressSettingsDialog.SHARED_PREFS_KEY_INTRO_REVIEWING, DEFAULT_INTRO_REVIEWING),
            prefs.getInt(ProgressSettingsDialog.SHARED_PREFS_KEY_ADV_INCORRECT, DEFAULT_ADV_INCORRECT),
            prefs.getInt(ProgressSettingsDialog.SHARED_PREFS_KEY_ADV_REVIEWING, DEFAULT_ADV_REVIEWING),
            prefs.getInt(ProgressSettingsDialog.SHARED_PREFS_KEY_CHAR_COOLDOWN, DEFAULT_CHAR_COOLDOWN),
            prefs.getBoolean(ProgressSettingsDialog.SHARED_PREFS_KEY_SKIP_SRS_ON_FIRST_CORRECT, DEFAULT_SKIP_SRS_ON_FIRST_CORRECT));
    }

    private class ProcessLogRow implements DataHelper.ProcessRow {
        private final List<ProgressTracker> allPts;

        public ProcessLogRow(List<ProgressTracker> allPts) {
            this.allPts = allPts;
        }

        @Override
        public void process(Map<String, String> r) {
            Character character = r.get("character").charAt(0);
            String set = r.get("charset");
            String timestampStr = r.get("timestamp");
            Integer score = Integer.parseInt(r.get("score"));
            String iid = r.get("install_id");
            LocalDateTime t;

            try {
                t = LocalDateTime.parse(timestampStr, formatter);
            } catch(DateTimeParseException e){
                try {
                    t = LocalDateTime.parse(timestampStr.replace(' ', 'T'), formatter);
                } catch (DateTimeParseException x) {
                    UncaughtExceptionLogger.backgroundLogError("Error caught parsing timestamp on practice log: " + timestampStr, x, context);
                    return;
                }
            }

            for(ProgressTracker pt: allPts) {
                if(pt.reject(character)){
                    continue;
                }

                pt.noteTimestamp(character, t, iid);

                if(BuildConfig.DEBUG){
                    //Log.d("nakama-progress", "Processing practice log: " + character + " " + set + " " + timestampStr + " " + score + " on set tracker " + pt.setId);
                }

                if (character.equals('R')) {
                    // indicates reset progress for standardSets characters
                    pt.progressReset(context, set);
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
    }
}
