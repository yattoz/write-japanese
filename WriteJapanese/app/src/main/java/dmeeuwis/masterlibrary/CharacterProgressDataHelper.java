package dmeeuwis.masterlibrary; 
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import dmeeuwis.nakama.data.DataHelper;

public class CharacterProgressDataHelper extends DataHelper {
    private final Context context;
    private final WriteJapaneseOpenHelper dbhelper;
    
    public CharacterProgressDataHelper(Context c){
        this.context = c;
        Log.i("nakama", "Opening CharacterProgressDataHelper.");
        this.dbhelper = new WriteJapaneseOpenHelper(this.context);
    }
    
    public void close(){
        Log.i("nakama", "Closing CharacterProgressDataHelper.");
        this.dbhelper.close();
    }

    public void recordProgress(String charSet, String progressString){
    	String existing = getExistingProgress(charSet);
    	if(existing == null){
    		Log.i("nakama", "INSERT INTO character_progress(charset, progress) VALUES(?, ?)" + " " + charSet + "; " + progressString);
	    	this.getWritableDatabase().execSQL("INSERT INTO character_progress(charset, progress) VALUES(?, ?)", 
    			new String[] { charSet, progressString });
    	} else {
	    	this.getWritableDatabase().execSQL("UPDATE character_progress SET progress = ? WHERE charset = ?",  
	    			new String[] { progressString, charSet });
    	}
    }

    public void clearProgress(String charSet){
		this.getWritableDatabase().execSQL("DELETE FROM character_progress WHERE charset = ?", new String[] { charSet });
    }
    
    public String getExistingProgress(String charset){
    	 return selectStringOrNull("SELECT progress FROM character_progress WHERE charset = ?", charset);
    }
    
	@Override
    public SQLiteDatabase getWritableDatabase(){
		return this.dbhelper.getWritableDatabase();
	}    
}
