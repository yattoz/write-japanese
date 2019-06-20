package dmeeuwis.kanjimaster.logic.data;
import android.content.Context;

public class StoryDataHelper {
    private final Context context;

    public StoryDataHelper(Context c){
        if(c == null){ throw new IllegalArgumentException("Need a not-null context to construct StoryDataHelper"); }
        this.context = c;
    }
    
    public void recordStory(char character, String story){
        String existing = getStory(character);
        WriteJapaneseOpenHelper db = new WriteJapaneseOpenHelper(context);
        try {
            if (existing == null) {
                db.getWritableDatabase().execSQL("INSERT INTO kanji_stories(character, story, timestamp) VALUES(?, ?, CURRENT_TIMESTAMP)",
                        new String[]{Character.toString(character), story});
            } else {
                db.getWritableDatabase().execSQL("UPDATE kanji_stories SET story = ?, timestamp = CURRENT_TIMESTAMP WHERE character = ?",
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
