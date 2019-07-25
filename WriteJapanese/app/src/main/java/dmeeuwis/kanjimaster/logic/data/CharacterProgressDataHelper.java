package dmeeuwis.kanjimaster.logic.data;

import android.util.Log;
import android.util.Pair;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import dmeeuwis.kanjimaster.logic.drawing.PointDrawing;

public class CharacterProgressDataHelper {
    public final static int DEFAULT_INTRO_INCORRECT = 5;

    public final static int DEFAULT_INTRO_REVIEWING = 10;
    public final static int DEFAULT_ADV_INCORRECT = 1;
    public final static int DEFAULT_ADV_REVIEWING = 2;
    public final static int DEFAULT_CHAR_COOLDOWN = 4;
    public final static boolean DEFAULT_SKIP_SRS_ON_FIRST_CORRECT = true;

    private final UUID iid;

    public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    static String lastRowId = null;

    public CharacterProgressDataHelper(UUID iid){
        this.iid = iid;
    }

    public void  cachePracticeRecord(String setId, String practiceRecord, String srsQueue, String oldestLog){
        DataHelperFactory.get().execSQL("INSERT OR REPLACE INTO practice_record_cache(set_id, practice_record, srs_queue, last_log_by_device) VALUES(?, ?, ?, ?)",
                new String[]{ setId, practiceRecord, srsQueue, oldestLog });
    }

    public void srsReset(String charSet){
        DataHelperFactory.get().execSQL("INSERT INTO practice_log(id, install_id, character, charset, score) VALUES(?, ?, ?, ?, ?)",
                new String[]{ UUID.randomUUID().toString(), iid.toString(), "S", charSet, "0" });
    }

    public void clearProgress(String charSet){
        DataHelperFactory.get().execSQL("INSERT INTO practice_log(id, install_id, character, charset, score) VALUES(?, ?, ?, ?, ?)",
                new String[]{ UUID.randomUUID().toString(), iid.toString(), "R", charSet, "0" });
    }
    
    public String recordPractice(String charset, String character, PointDrawing d, int score){
        String serialized = null;
        if(d != null){
            serialized = d.serialize();
        }
        String rowId = UUID.randomUUID().toString();
            DataHelperFactory.get().execSQL("INSERT INTO practice_log(id, install_id, character, charset, score, drawing) VALUES(?, ?, ?, ?, ?, ?)",
                    new String[]{rowId, iid.toString(), character, charset, Integer.toString(score), serialized });
        lastRowId = rowId;
        return rowId;
    }

    public void recordGoals(String charset, GregorianCalendar goalStart, GregorianCalendar goal) {
        SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd");

       if(goal== null){
           DataHelperFactory.get().execSQL("DELETE FROM charset_goals WHERE charset = ?",
                   new String[]{charset});
            return;
       }

        String goalStartStr = sd.format(goalStart.getTime());
        String goalStr = sd.format(goal.getTime());
        Pair<GregorianCalendar, GregorianCalendar> goals = getExistingGoals(charset);
        if (goals == null) {
            DataHelperFactory.get().execSQL("INSERT INTO charset_goals(charset, goal_start, goal, timestamp) VALUES(?, ?, ?, current_timestamp)",
                    new String[]{charset, goalStartStr, goalStr});
        } else {
            DataHelperFactory.get().execSQL("UPDATE charset_goals SET goal_start = ?, goal = ?, timestamp=current_timestamp WHERE charset = ?",
                    new String[]{goalStartStr, goalStr, charset});
        }
    }

    public Pair<GregorianCalendar, GregorianCalendar> getExistingGoals(String charset){
        Map<String, String> rec = DataHelperFactory.get().selectRecord(
                "SELECT goal_start, goal FROM charset_goals WHERE charset = ? ORDER BY timestamp DESC LIMIT 1", charset);
        if(rec == null) { return null; }
        return Pair.create(parseCalendarString(rec.get("goal_start")), parseCalendarString(rec.get("goal")));
    }


    public void loadProgressTrackerFromDB(final List<ProgressTracker> allPts, ProgressCacheFlag cacheFlag){
        loadProgressTrackerFromDB(allPts, false, cacheFlag);
    }

    public void resumeProgressTrackerFromDB(List<ProgressTracker> allPts) {
        loadProgressTrackerFromDB(allPts, true, ProgressCacheFlag.USE_RAW_LOGS);
    }

    public Map<String, String> countPracticeLogs() {
        return DataHelperFactory.get().selectRecord(
                "SELECT charset, count(*) from practice_log");
    }

    public enum ProgressCacheFlag { USE_CACHE, USE_RAW_LOGS }

    private void loadProgressTrackerFromDB(final List<ProgressTracker> allPtsOrig, boolean resuming, ProgressCacheFlag useCache ){
        long start = System.currentTimeMillis();

        List<ProgressTracker> allPts = new ArrayList<>(allPtsOrig);

        long startResume = System.currentTimeMillis();
        fromCache: if(!resuming && useCache == ProgressCacheFlag.USE_CACHE){
            // check timestamp of log vs json

            Map<String, Map<String, String>> caches = DataHelperFactory.get().selectRecordsIndexedByFirst(
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

        DataHelper.ProcessRow plr = ProcessLogRowFactory.get();
        if(resuming){

            for(ProgressTracker pt: allPts){
                for(Map.Entry<String, LocalDateTime> e: pt.oldestLogTimestampByDevice.entrySet()){

                    long queryStart = System.currentTimeMillis();
                    String timestamp = e.getValue() == null ? "1900-01-01 00:00:00" : e.getValue().toString().replace('T', ' ');
                    int rowCount = plr.applyToResults(
                            "SELECT character, charset, score, timestamp, install_id FROM practice_log WHERE install_id = ? AND timestamp > datetime(?) and charset = ?",
                            pt,
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
            for(ProgressTracker pt: allPts) {
                allCount = plr.applyToResults("SELECT character, charset, score, timestamp, install_id FROM practice_log", pt);
            }
        }
        Log.d("nakama-progress", "Time to do process block: " + (System.currentTimeMillis() - processStart));

        long startup = System.currentTimeMillis() - start;
        Log.i("nakama-progress", "Time to load progress tracker: " + startup + "ms; counted " + allCount + " records.");
        if(startup > 2500){
            UncaughtExceptionLogger.backgroundLogError("Long startup detected: " + startup + "ms to load " + allCount + " practice logs.", new RuntimeException());
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

    public String overRideLast() {
        if(lastRowId != null) {
            Integer last = DataHelperFactory.get().selectInteger(
                    "SELECT score FROM practice_log WHERE id = ?", lastRowId);
            Integer next = last == 100 ? 0 : 100;
            DataHelperFactory.get().selectRecord(
                    "UPDATE practice_log SET score = ? WHERE id = ?", next, lastRowId);
            return lastRowId;
        }
        return null;
    }

    public void clearPracticeRecord() {
        DataHelperFactory.get().execSQL("DELETE FROM practice_record_cache");
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
}
