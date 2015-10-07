package dmeeuwis.nakama.data;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CharacterProgressDataHelper {
    private final Context context;
    private final UUID iid;

    public CharacterProgressDataHelper(Context c, UUID iid){
        Log.i("nakama", "Opening CharacterProgressDataHelper.");
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
    
    public void recordPractice(String charset, String character, int score){
        WriteJapaneseOpenHelper db = new WriteJapaneseOpenHelper(this.context);
        Log.i("nakama-record", "Recording practice: " + charset + "; " + character + "; " + score);
        try {
            db.getWritableDatabase().execSQL("INSERT INTO practice_log(id, install_id, character, charset, score) VALUES(?, ?, ?, ?, ?)",
                    new String[]{UUID.randomUUID().toString(), iid.toString(), character, charset, Integer.toString(score) });
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

    public Map<Character, Integer> getRecordSheetForCharset(String charsetName){
        long start = System.currentTimeMillis();

        WriteJapaneseOpenHelper db = new WriteJapaneseOpenHelper(this.context);
        Map<Character, Integer> recordSheet = new HashMap<>();
        try {
            List<Map<String, String>> rec = DataHelper.selectRecords(db.getReadableDatabase(),
                    "SELECT character, score FROM practice_log WHERE charset = ?", charsetName);
            Log.i("nakama-record", "-----> Found " + rec.size() + " practice logs to read");
            for(Map<String, String> r: rec) {
                Character character = r.get("character").charAt(0);
                Integer score = Integer.parseInt(r.get("score"));

                Integer sheetScore;
                if(character.toString().equals("R") && score.intValue() == 0){
                    // indicates reset progress for all characters
                    recordSheet.clear();
                } else {
                    sheetScore = recordSheet.get(character);
                    if (sheetScore != null) {
                        sheetScore = Math.max(0, Math.min(200, sheetScore + score));
                    } else {
                        sheetScore = score;
                    }
                    recordSheet.put(character, sheetScore);
                }
            }
        } finally {
            db.close();
        }

        Log.i("nakama-progress", "Time to load record sheet: " + (System.currentTimeMillis() - start) + "ms");
        return recordSheet;
    }

    private static GregorianCalendar parseCalendarString(String in){
        if(in == null) { return null; }
        String[] parts = in.split("-");
        return new GregorianCalendar(Integer.valueOf(parts[0]), Integer.valueOf(parts[1])-1, Integer.valueOf(parts[2]));
    }
}
