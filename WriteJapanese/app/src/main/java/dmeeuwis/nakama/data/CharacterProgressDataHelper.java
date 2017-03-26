package dmeeuwis.nakama.data;

import android.content.Context;
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

public class CharacterProgressDataHelper {
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

    public Map<Character, Integer> getRecordSheetForCharset(final Set<Character> validChars){
        long start = System.currentTimeMillis();

        WriteJapaneseOpenHelper db = new WriteJapaneseOpenHelper(this.context);
        final Map<Character, Integer> recordSheet = new HashMap<>();
        try {
            DataHelper.applyToResults(
                new DataHelper.ProcessRow() {
                    @Override
                    public void process(Map<String, String> r) {
                        Character character = r.get("character").charAt(0);
                        if(!validChars.contains(character)){
                            return;
                        }

                        Integer scoreRaw = Integer.parseInt(r.get("score"));
                        Integer score = scoreRaw == 0 ? 0 :
                                        scoreRaw <  0 ? -1 :
                                        1;

                        Integer sheetScore;
                        if(character.toString().equals("R") && score == 0){
                            // indicates reset progress for all characters
                            recordSheet.clear();
                        } else {
                            sheetScore = recordSheet.get(character);
                            sheetScore = (sheetScore == null ? 0 : sheetScore);
                            sheetScore = Math.max(-2, Math.min(2, sheetScore + score));

                            recordSheet.put(character, sheetScore);
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
}
