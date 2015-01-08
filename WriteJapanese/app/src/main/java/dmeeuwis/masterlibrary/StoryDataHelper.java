package dmeeuwis.masterlibrary; 
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import dmeeuwis.nakama.data.DataHelper;

public class StoryDataHelper {
    private final Context context;

    public StoryDataHelper(Context c){
        this.context = c;
    }
    
    public void recordStory(char character, String story){
        WriteJapaneseOpenHelper db = new WriteJapaneseOpenHelper(context);
        try {
            String existing = getStory(character);
            if (existing == null) {
                db.getWritableDatabase().execSQL("INSERT INTO kanji_stories(character, story) VALUES(?, ?)",
                        new String[]{Character.toString(character), story});
            } else {
                db.getWritableDatabase().execSQL("UPDATE kanji_stories SET story = ? WHERE character = ?",
                        new String[]{story, String.valueOf(character)});

            }
        } finally {
            db.close();;
        }
    }
    
    public String getStory(char character){
        WriteJapaneseOpenHelper db = new WriteJapaneseOpenHelper(context);
        try {
            return DataHelper.selectStringOrNull(db.getReadableDatabase(), "SELECT story FROM kanji_stories WHERE character = ?", Character.toString(character));
        } finally {
            db.close();
        }
	}
}
