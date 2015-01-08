package dmeeuwis.masterlibrary; 
import android.content.Context;
import android.util.Log;
import dmeeuwis.nakama.data.DataHelper;

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
}
