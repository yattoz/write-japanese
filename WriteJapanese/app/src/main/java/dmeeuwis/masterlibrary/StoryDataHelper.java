package dmeeuwis.masterlibrary; 
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import dmeeuwis.nakama.data.DataHelper;

public class StoryDataHelper extends DataHelper {
    private final Context context;
    private final WriteJapaneseOpenHelper dbhelper;
    
    public StoryDataHelper(Context c){
        this.context = c;
        Log.i("nakama", "Opening StoryDataHelper.");
        this.dbhelper = new WriteJapaneseOpenHelper(this.context);
    }
    
    public void close(){
        Log.i("nakama", "Closing StoryDataHelper.");
        this.dbhelper.close();
    }

    public void recordStory(char character, String story){
    	String existing = getStory(character);
    	if(existing == null){
	    	this.getWritableDatabase().execSQL("INSERT INTO kanji_stories(character, story) VALUES(?, ?)", 
    			new String[] { Character.toString(character), story });
    	} else {
	    	this.getWritableDatabase().execSQL("UPDATE kanji_stories SET story = ? WHERE character = ?",  
	    			new String[] { story, String.valueOf(character) });
    		
    	}
    }
    
    public String getStory(char character){
    	 return selectStringOrNull("SELECT story FROM kanji_stories WHERE character = ?", Character.toString(character));
    }
    
	@Override
    public SQLiteDatabase getWritableDatabase(){
		return this.dbhelper.getWritableDatabase();
	}    
}
