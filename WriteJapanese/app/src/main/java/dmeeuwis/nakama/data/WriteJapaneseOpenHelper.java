package dmeeuwis.nakama.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class WriteJapaneseOpenHelper extends SQLiteOpenHelper {
	private static final String DB_NAME = "write_japanese.db";
	private static final int DB_VERSION = 4;

	public WriteJapaneseOpenHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase dbase) {
		Log.d("nakama", "Creating story table.");
		dbase.execSQL("CREATE TABLE kanji_stories ( " +
                "character char NOT NULL PRIMARY KEY, " +
                "story TEXT NOT NULL" +
        ")");

		Log.d("nakama", "Creating character progress table.");
		dbase.execSQL("CREATE TABLE character_progress ( " +
                "charset TEXT NOT NULL PRIMARY KEY, " +
                "progress TEXT NOT NULL" +
        ")");

        createCharset(dbase);
	}

    private void createCharset(SQLiteDatabase sqlite){
        Log.d("nakama", "Creating character progress table.");
        sqlite.execSQL("CREATE TABLE charset_goals ( " +
                "charset TEXT NOT NULL PRIMARY KEY, " +
                "goal_start TEXT NOT NULL," +
                "goal TEXT NOT NULL" +
                ")");
    }

	@Override
	public void onUpgrade(SQLiteDatabase dbase, int oldVersion, int newVersion) {
		Log.i("nakama", "Upgrading db from " + oldVersion + " to " + newVersion);

        if(oldVersion <= 3){
           createCharset(dbase);
        }
	}
}