package dmeeuwis.nakama.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Pair;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import dmeeuwis.nakama.kanjidraw.PointDrawing;
import dmeeuwis.nakama.primary.ProgressSettingsDialog;

public class CharacterProgressDataHelper {
    public final static int DEFAULT_INTRO_INCORRECT = 5;
    public final static int DEFAULT_INTRO_REVIEWING = 10;
    public final static int DEFAULT_ADV_INCORRECT = 1;
    public final static int DEFAULT_ADV_REVIEWING = 2;

    private final Context context;
    private final UUID iid;

    public CharacterProgressDataHelper(Context c, UUID iid){
        this.context = c;
        this.iid = iid;
    }
    
    public void clearProgress(String charSet){
        WriteJapaneseOpenHelper db = new WriteJapaneseOpenHelper(this.context);
        try {
            // 0 to indicate reset progress? Am I being silly...?
            db.getWritableDatabase().execSQL("INSERT INTO practice_log(id, install_id, character, charset, score) VALUES(?, ?, ?, ?, ?)",
                    new String[]{ UUID.randomUUID().toString(), iid.toString(), "R", charSet, "0" });
        } finally {
            db.close();
        }
    }
    
    public void recordPractice(String charset, String character, PointDrawing d, int score){
        String serialized = d.serialize();
        WriteJapaneseOpenHelper db = new WriteJapaneseOpenHelper(this.context);
        //Log.i("nakama-record", "Recording practice: " + charset + "; " + character + "; " + score + "; drawing: " + serialized);
        try {
            db.getWritableDatabase().execSQL("INSERT INTO practice_log(id, install_id, character, charset, score, drawing) VALUES(?, ?, ?, ?, ?, ?)",
                    new String[]{UUID.randomUUID().toString(), iid.toString(), character, charset, Integer.toString(score), serialized });
        } finally {
            db.close();
        }
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

    public Map<Character, Integer> getRecordSheetForCharset(final Set<Character> validChars, final int advanceIncorrect){
        long start = System.currentTimeMillis();

        WriteJapaneseOpenHelper db = new WriteJapaneseOpenHelper(this.context);
        final Map<Character, Integer> recordSheet = new HashMap<>();
        try {
            DataHelper.applyToResults(
                new DataHelper.ProcessRow() {
                    @Override
                    public void process(Map<String, String> r) {
                        Character character = r.get("character").charAt(0);

                        if(character.toString().equals("R")){
                            // indicates reset progress for all characters
                            recordSheet.clear();
                        } else {
                            if(!validChars.contains(character)){
                                return;
                            }

                            Integer score = Integer.parseInt(r.get("score"));
                            if(score == 100){
                                Integer sheetScore = recordSheet.get(character);
                                sheetScore = sheetScore == null ? 0 : sheetScore;
                                recordSheet.put(character, Math.min(0, 1 + sheetScore));
                                Log.d("nakama-progression", "Good history puts " + character + " at " + recordSheet.get(character));
                            } else {
                                recordSheet.put(character, -1 * advanceIncorrect);
                                Log.d("nakama-progression", "Bad history puts " + character + " at " + recordSheet.get(character));
                            }

                        }
                    }
                },

                db.getReadableDatabase(),
                "SELECT character, score FROM practice_log");
        } finally {
            db.close();
        }

        Log.i("nakama-progress", "Time to load record sheet: " + (System.currentTimeMillis() - start) + "ms");
        return recordSheet;
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
}
