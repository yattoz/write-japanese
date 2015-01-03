package dmeeuwis.masterlibrary;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class WriteJapaneseOpenHelper extends SQLiteOpenHelper {
	private static final String DB_NAME = "write_japanese.db";
	private static final int DB_VERSION = 3;

	public WriteJapaneseOpenHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase dbase) {
		Log.d("nakama", "Creating story table.");
		dbase.execSQL("CREATE TABLE kanji_stories ( " + "character char NOT NULL PRIMARY KEY, " + "story TEXT NOT NULL" + ")");

		Log.d("nakama", "Creating character progress table.");
		dbase.execSQL("CREATE TABLE character_progress ( " + "charset TEXT NOT NULL PRIMARY KEY, " + "progress TEXT NOT NULL" + ")");
	}

	@Override
	public void onUpgrade(SQLiteDatabase dbase, int oldVersion, int newVersion) {
		Log.i("nakama", "Upgrading db from " + +oldVersion + " to " + newVersion);
		try {
			dbase.execSQL("DROP TABLE kanji_stories");
		} catch(Throwable t){
			// ignore
		}
		try {
			dbase.execSQL("DROP TABLE character_progress");
		} catch(Throwable t){
			// ignore
		}
	}
}