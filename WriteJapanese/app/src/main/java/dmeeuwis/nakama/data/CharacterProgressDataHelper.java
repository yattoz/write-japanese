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
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import dmeeuwis.kanjimaster.BuildConfig;
import dmeeuwis.nakama.kanjidraw.PointDrawing;
import dmeeuwis.nakama.primary.ProgressSettingsDialog;

public class CharacterProgressDataHelper {
    public final static int DEFAULT_INTRO_INCORRECT = 5;
    public final static int DEFAULT_INTRO_REVIEWING = 10;
    public final static int DEFAULT_ADV_INCORRECT = 1;
    public final static int DEFAULT_ADV_REVIEWING = 2;

    private final Context context;
    private final UUID iid;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    String lastRowId = null;

    public CharacterProgressDataHelper(Context c, UUID iid){
        this.context = c;
        this.iid = iid;
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
                    "SELECT goal_start, goal FROM charset_goals WHERE charset = ? ORDER BY timestamp LIMIT 1", charset);
            if(rec == null) { return null; }
            return Pair.create(parseCalendarString(rec.get("goal_start")), parseCalendarString(rec.get("goal")));
        } finally {
            db.close();
        }
    }


    public void loadProgressTrackerFromDB(final List<ProgressTracker> allPts){
        loadProgressTrackerFromDB(allPts, false);
    }

    public void resumeProgressTrackerFromDB(List<ProgressTracker> allPts) {
        loadProgressTrackerFromDB(allPts, true);
    }

    private void loadProgressTrackerFromDB(final List<ProgressTracker> allPts, final boolean resuming){
        long start = System.currentTimeMillis();

        WriteJapaneseOpenHelper db = new WriteJapaneseOpenHelper(this.context);
        final AtomicLong count = new AtomicLong(0);

        LocalDateTime oldestLog = LocalDateTime.of(2000, 1, 1, 0, 0);
        if(resuming) {
            for (ProgressTracker p : allPts) {
                if (p != null && p.oldestLogTimestamp != null && oldestLog.isBefore(p.oldestLogTimestamp)) {
                    oldestLog = p.oldestLogTimestamp;
                }
            }
            Log.i("nakama-progress", "Saw oldest log from " + allPts.size() + " sets as " + oldestLog);
        }

        try {
            ProcessLogRow plr = new ProcessLogRow(count, allPts);
            if(resuming){
                Log.i("nakama-progress", "Selecting all logs since " + oldestLog);
                DataHelper.applyToResults(plr, db.getReadableDatabase(), "SELECT character, charset, score, timestamp FROM practice_log WHERE timestamp > datetime(?)", oldestLog.toString());
            } else {
                DataHelper.applyToResults(plr, db.getReadableDatabase(), "SELECT character, charset, score, timestamp FROM practice_log");
            }
        } finally {
            db.close();
        }

        long startup = System.currentTimeMillis() - start;
        Log.i("nakama-progress", "Time to load progress tracker: " + startup + "ms; counted " + count.get() + " records.");
        if(startup > 2500){
            UncaughtExceptionLogger.backgroundLogError("Long startup detected: " + startup + "ms to load " + count.get() + " practice logs.", new RuntimeException(), context);
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


    public static class ProgressionSettings {
        public final int introIncorrect, introReviewing, advanceIncorrect, advanceReviewing;

        public ProgressionSettings(int introIncorrect, int introReviewing, int advanceIncorrect, int advanceReviewing) {
            this.introIncorrect = introIncorrect;
            this.introReviewing = introReviewing;
            this.advanceIncorrect = advanceIncorrect;
            this.advanceReviewing = advanceReviewing;
        }
    }

    public ProgressionSettings getProgressionSettings(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return new ProgressionSettings(
            prefs.getInt(ProgressSettingsDialog.SHARED_PREFS_KEY_INTRO_INCORRECT, DEFAULT_INTRO_INCORRECT),
            prefs.getInt(ProgressSettingsDialog.SHARED_PREFS_KEY_INTRO_REVIEWING, DEFAULT_INTRO_REVIEWING),
            prefs.getInt(ProgressSettingsDialog.SHARED_PREFS_KEY_ADV_INCORRECT, DEFAULT_ADV_INCORRECT),
            prefs.getInt(ProgressSettingsDialog.SHARED_PREFS_KEY_ADV_REVIEWING, DEFAULT_ADV_REVIEWING));
    }

    private class ProcessLogRow implements DataHelper.ProcessRow {
        private final AtomicLong count;
        private final List<ProgressTracker> allPts;

        public ProcessLogRow(AtomicLong count, List<ProgressTracker> allPts) {
            this.count = count;
            this.allPts = allPts;
        }

        @Override
        public void process(Map<String, String> r) {
            count.incrementAndGet();

            Character character = r.get("character").charAt(0);
            String set = r.get("charset");
            String timestampStr = r.get("timestamp");
            Integer score = Integer.parseInt(r.get("score"));
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
                pt.noteTimestamp(t);


                if(BuildConfig.DEBUG){
                    Log.d("nakama-progress", "Processing practice log: " + character + " " + set + " " + timestampStr + " " + score + " on set tracker " + pt.setId);
                }

                if (character.toString().equals("R")) {
                    // indicates reset progress for standardSets characters
                    pt.progressReset(context, set);
                    //Log.d("nakama-progress", "Loaded PROGRESS RESET for set " + set);

                } else if (character.toString().equals("S")) {
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
