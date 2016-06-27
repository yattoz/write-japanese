package dmeeuwis.nakama.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import dmeeuwis.nakama.primary.Iid;

public class WriteJapaneseOpenHelper extends SQLiteOpenHelper {
	private static final String DB_NAME = "write_japanese.db";
	private static final int DB_VERSION = 17;

    private final String iid;

	public WriteJapaneseOpenHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
        this.iid = Iid.get(context).toString();
    }

	@Override
	public void onCreate(SQLiteDatabase dbase) {
        createStoryTables(dbase);
        createCharset(dbase);
        createPracticeLog(dbase);
        addTimestampToStories(dbase);
        addDrawingToPracticeLog(dbase);
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

    private void addDrawingToPracticeLog(SQLiteDatabase sqlite){
        Log.d("nakama-db", "Creating practice log table.");
        sqlite.execSQL("ALTER TABLE practice_log ADD COLUMN drawing TEXT");
    }

    private void migratePracticeTrackerToPracticeLogs(SQLiteDatabase sqlite){
        List<Map<String, String>> rows = DataHelper.selectRecords(sqlite, "SELECT * FROM character_progress");
        for(Map<String, String> r: rows){
            String charset = r.get("charset");
            String record = r.get("progress");
            for(String line: record.split("\n")){
                try {
                    String[] parts = line.split("=");
                    String character =parts[0];
                    if("!".equals(parts[1])) {
                        continue;
                    }
                    Integer value = Integer.parseInt(parts[1]);

                    if(value == -2){
                        sqlite.execSQL("INSERT INTO practice_log(id, install_id, character, charset, score) VALUES(?, ?, ?, ?, ?)", new String[]{UUID.randomUUID().toString(), iid, character, charset, "-200"});
                    } else if (value == -1){
                        sqlite.execSQL("INSERT INTO practice_log(id, install_id, character, charset, score) VALUES(?, ?, ?, ?, ?)", new String[]{UUID.randomUUID().toString(), iid, character, charset, "-200"});
                        sqlite.execSQL("INSERT INTO practice_log(id, install_id, character, charset, score) VALUES(?, ?, ?, ?, ?)", new String[]{UUID.randomUUID().toString(), iid, character, charset, "100"});
                    } else if (value >= 1){
                        sqlite.execSQL("INSERT INTO practice_log(id, install_id, character, charset, score) VALUES(?, ?, ?, ?, ?)", new String[]{UUID.randomUUID().toString(), iid, character, charset, "100"});
                    }
                } catch(Throwable t){
                    Log.e("nakama", "Error parsing record line: " + line, t);
                }
            }
        }
    }

    private void addTimestampToStories(SQLiteDatabase sqlite){
        Log.d("nakama-db", "Adding timestamp to stories table");
        sqlite.execSQL("ALTER TABLE kanji_stories ADD COLUMN timestamp DATETIME");
        sqlite.execSQL("UPDATE kanji_stories SET timestamp = CURRENT_TIMESTAMP");
        // unfortunately, cannot add column with default CURRENT_TIMESTAMP due to sqlite limitation
    }


	@Override
	public void onUpgrade(SQLiteDatabase dbase, int oldVersion, int newVersion) {
		Log.i("nakama-db", "Upgrading db from " + oldVersion + " to " + newVersion);

        if(oldVersion <= 7){
           createCharset(dbase);
        }

        if(oldVersion <= 11){
            createPracticeLog(dbase);
            try {
                migratePracticeTrackerToPracticeLogs(dbase);
            } catch(Throwable t){
                Log.e("nakama", "Error during progress migration", t);
            }
        }

        if(oldVersion <= 13){
            addTimestampToStories(dbase);
        }

        if(oldVersion <= 16){
            addDrawingToPracticeLog(dbase);
        }
	}
}