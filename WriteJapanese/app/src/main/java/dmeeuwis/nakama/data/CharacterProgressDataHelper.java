package dmeeuwis.nakama.data;
import android.content.Context;
import android.util.Log;
import android.util.Pair;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Map;

public class CharacterProgressDataHelper {
    private final Context context;

    public CharacterProgressDataHelper(Context c){
        Log.i("nakama", "Opening CharacterProgressDataHelper.");
        this.context = c;
    }
    
    public void recordProgress(String charSet, String progressString){
        WriteJapaneseOpenHelper db = new WriteJapaneseOpenHelper(this.context);
        try {
            String existing = getExistingProgress(charSet);
            if (existing == null) {
                Log.i("nakama", "INSERT INTO character_progress(charset, progress) VALUES(?, ?)" + " " + charSet + "; " + progressString);
                db.getWritableDatabase().execSQL("INSERT INTO character_progress(charset, progress) VALUES(?, ?)",
                        new String[]{charSet, progressString});
            } else {
                db.getWritableDatabase().execSQL("UPDATE character_progress SET progress = ? WHERE charset = ?",
                        new String[]{progressString, charSet});
            }
        } finally {
            db.close();
        }
    }

    public void clearProgress(String charSet){
        WriteJapaneseOpenHelper db = new WriteJapaneseOpenHelper(this.context);
        try {
            db.getWritableDatabase().execSQL("DELETE FROM character_progress WHERE charset = ?", new String[]{charSet});
        } finally {
            db.close();
        }
    }
    
    public String getExistingProgress(String charset){
        WriteJapaneseOpenHelper db = new WriteJapaneseOpenHelper(this.context);
        try {
            return DataHelper.selectStringOrNull(db.getReadableDatabase(), "SELECT progress FROM character_progress WHERE charset = ?", charset);
        } finally {
            db.close();
        }
    }


    public void recordPractice(String charset, String character, int score){
        WriteJapaneseOpenHelper db = new WriteJapaneseOpenHelper(this.context);
        try {
            db.getWritableDatabase().execSQL("INSERT INTO practice_log(character, charset, timestamp, score) VALUES(?, ?, NOW(), ?)",
                    new String[]{ character, charset, Integer.toString(score) });
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
                db.getWritableDatabase().execSQL("INSERT INTO charset_goals(charset, goal_start, goal) VALUES(?, ?, ?)",
                        new String[]{charset, goalStartStr, goalStr});
            } else {
                db.getWritableDatabase().execSQL("UPDATE charset_goals SET goal_start = ?, goal = ? WHERE charset = ?",
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
                    "SELECT goal_start, goal FROM charset_goals WHERE charset = ?", charset);
            if(rec == null) { return null; }
            return Pair.create(parseCalendarString(rec.get("goal_start")), parseCalendarString(rec.get("goal")));
        } finally {
            db.close();
        }
    }

    private static GregorianCalendar parseCalendarString(String in){
        if(in == null) { return null; }
        String[] parts = in.split("-");
        return new GregorianCalendar(Integer.valueOf(parts[0]), Integer.valueOf(parts[1])-1, Integer.valueOf(parts[2]));
    }
}
