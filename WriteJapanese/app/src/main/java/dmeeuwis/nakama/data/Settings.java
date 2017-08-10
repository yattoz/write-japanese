package dmeeuwis.nakama.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import dmeeuwis.nakama.primary.Iid;
import dmeeuwis.nakama.primary.IntroActivity;

public class Settings {
    public static Map<String, String> settingsCache = new ConcurrentHashMap<>();

    public static boolean getSRSEnabled(Context ctx) {
        return getBooleanSetting(ctx, IntroActivity.USE_SRS_SETTING_NAME, true);
    }

    public static boolean getSRSNotifications(Context ctx) {
        return getBooleanSetting(ctx, IntroActivity.SRS_NOTIFICATION_SETTING_NAME, true);
    }

    public static boolean getSRSAcrossSets(Context ctx) {
        return getBooleanSetting(ctx, IntroActivity.SRS_ACROSS_SETS, true);
    }

    public enum Strictness { CASUAL, CASUAL_ORDERED, STRICT }
    public enum ClueType { MEANING, VOCAB }

    public static Strictness getStrictness(Context appContext){
        return Strictness.valueOf(getSetting("strictness", Strictness.CASUAL.toString(), appContext));
    }

    public static void setStrictness(Strictness s, Context appContext){
        setSetting("strictness", s.toString(), appContext);
    }

    public static String getStorySharing(Context appContext){
        return getSetting("story_sharing", null, appContext);

    }

    public static void setStorySharing(String value, Context appContext){
        setSetting("story_sharing", value, appContext);
    }

    public static void setClueType(ClueType value, Context appContext){
        setSetting("clue_type", value.toString(), appContext);
    }

    public static ClueType getClueType(Context appContext){
        return ClueType.valueOf(getSetting("clue_type", ClueType.MEANING.toString(), appContext));
    }

    public static void setBooleanSetting(Context appContext, String name, boolean value){
        setSetting(name, Boolean.toString(value), appContext);
    }

    public static Boolean getBooleanSetting(Context appContext, String name, boolean def){
        return Boolean.parseBoolean(getSetting(name, Boolean.toString(def), appContext));
    }

    public static String getSetting(String key, String defaultValue, Context appContext){
        WriteJapaneseOpenHelper dbh = new WriteJapaneseOpenHelper(appContext);
        try {
            SQLiteDatabase db = dbh.getReadableDatabase();
            Map<String, String> v = DataHelper.selectRecord(db, "SELECT value FROM settings_log WHERE setting = ? ORDER BY timestamp DESC LIMIT 1",
                    new String[]{ key });
            if(v == null){
                return defaultValue;
            }
            return v.get("value");
        } finally {
            dbh.close();
        }
    }

    public static void setSetting(String key, String value, Context appContext){
        WriteJapaneseOpenHelper dbh = new WriteJapaneseOpenHelper(appContext);
        try {
            SQLiteDatabase db = dbh.getWritableDatabase();
            Map<String, String> v = DataHelper.selectRecord(db,
                    "INSERT INTO settings_log(id, install_id, timestamp, setting, value) VALUES(?, ?, CURRENT_TIMESTAMP, ?, ?)",
                    new String[]{UUID.randomUUID().toString(), Iid.get(appContext).toString(), key, value });
        } finally {
            dbh.close();
        }
    }
}
