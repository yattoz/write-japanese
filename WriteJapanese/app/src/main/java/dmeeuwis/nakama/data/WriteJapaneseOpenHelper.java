package dmeeuwis.nakama.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class WriteJapaneseOpenHelper extends SQLiteOpenHelper {
	private static final String DB_NAME = "write_japanese.db";
	private static final int DB_VERSION = 12;

	public WriteJapaneseOpenHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase dbase) {
        createProgressTable(dbase);
        createStoryTables(dbase);
        createCharset(dbase);
        createPracticeLog(dbase);
	}

    private void createProgressTable(SQLiteDatabase dbase){
        Log.d("nakama-db", "Creating character progress table.");
        dbase.execSQL("CREATE TABLE character_progress ( " +
                "charset TEXT NOT NULL PRIMARY KEY, " +
                "progress TEXT NOT NULL" +
                ")");

    }
    private void createStoryTables(SQLiteDatabase dbase){
        Log.d("nakama-db", "Creating story table.");
        dbase.execSQL("CREATE TABLE kanji_stories ( " +
                "character char NOT NULL PRIMARY KEY, " +
                "story TEXT NOT NULL" +
                ")");
    }

    private void createCharset(SQLiteDatabase sqlite){
        Log.d("nakama-db", "Creating charset goals table.");
        sqlite.execSQL("DROP TABLE IF EXISTS charset_goals;");
        sqlite.execSQL("CREATE TABLE charset_goals ( " +
                "charset TEXT NOT NULL PRIMARY KEY, " +
                "timestamp TEXT NOT NULL," +
                "goal_start TEXT NOT NULL," +
                "goal TEXT NOT NULL" +
        ")");
    }

    private void createPracticeLog(SQLiteDatabase sqlite){
        Log.d("nakama-db", "Creating practice log table.");
        sqlite.execSQL("DROP TABLE IF EXISTS practice_log;");
        sqlite.execSQL("CREATE TABLE practice_log ( " +
                "id TEXT NOT NULL PRIMARY KEY, " +
                "install_id TEXT NOT NULL, " +
                "character TEXT NOT NULL, " +
                "charset TEXT NOT NULL, " +
                "timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "score TEXT NOT NULL" +
        ")");

    }

    private void addTimestampToStories(SQLiteDatabase sqlite){
        Log.d("nakama-db", "Adding timestamp to stories table");
        sqlite.execSQL("ALTER TABLE kanji_stories ADD COLUMN creation_time TIMESTAMP");
        sqlite.execSQL("UPDATE kanji_stories SET creation_time = CURRENT_TIMESTAMP");
    }


	@Override
	public void onUpgrade(SQLiteDatabase dbase, int oldVersion, int newVersion) {
		Log.i("nakama-db", "Upgrading db from " + oldVersion + " to " + newVersion);

        if(oldVersion <= 7){
           createCharset(dbase);
        }

        if(oldVersion <= 11){
            createPracticeLog(dbase);
        }

        if(oldVersion <= 12){
            addTimestampToStories(dbase);
        }
	}
}