package dmeeuwis.nakama.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.util.Pair;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import dmeeuwis.nakama.primary.Iid;
import dmeeuwis.nakama.primary.IntroActivity;
import dmeeuwis.nakama.views.translations.ClueCard;

public class Settings {
    public static Boolean getSRSEnabled(Context ctx) {
        return getBooleanSetting(ctx, IntroActivity.USE_SRS_SETTING_NAME, null);
    }

    public static Boolean getSRSNotifications(Context ctx) {
        return getBooleanSetting(ctx, IntroActivity.SRS_NOTIFICATION_SETTING_NAME, null);
    }

    public static Boolean getSRSAcrossSets(Context ctx) {
        return getBooleanSetting(ctx, IntroActivity.SRS_ACROSS_SETS, null);
    }

    public static void setCrossDeviceSyncAsked(Context applicationContext) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext);
        SharedPreferences.Editor ed = prefs.edit();
        ed.putBoolean(SyncRegistration.HAVE_ASKED_ABOUT_SYNC_KEY, true);
        ed.apply();
    }


    public static void clearCrossDeviceSync(Context applicationContext) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext);
        SharedPreferences.Editor ed = prefs.edit();
        ed.remove(SyncRegistration.HAVE_ASKED_ABOUT_SYNC_KEY);
        ed.remove(SyncRegistration.AUTHCODE_SHARED_PREF_KEY);
        ed.apply();
    }


    public static void clearSRSSettings(Context applicationContext) {
        Settings.setSetting(IntroActivity.USE_SRS_SETTING_NAME, "clear", applicationContext);
        Settings.setSetting(IntroActivity.SRS_ACROSS_SETS, "clear", applicationContext);
    }

    public static class SyncStatus {
        public final boolean asked;
        public final String authcode;

        SyncStatus(boolean asked, String authcode){
            this.asked = asked;
            this.authcode = authcode;
        }

        public String toString(){
            return String.format("[SyncStatus asked=%b authcode=%s]", asked, authcode);
        }
    }

    public static SyncStatus getCrossDeviceSyncEnabled(Context applicationContext) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext);
        Boolean asked = prefs.getBoolean(SyncRegistration.HAVE_ASKED_ABOUT_SYNC_KEY, false);
        String authcode = prefs.getString(SyncRegistration.AUTHCODE_SHARED_PREF_KEY, null);
        return new SyncStatus(asked, authcode);
    }

    public enum Strictness { CASUAL, CASUAL_ORDERED, STRICT }

    public static Strictness getStrictness(Context appContext){
        return Strictness.valueOf(getSetting("strictness", Strictness.CASUAL.toString(), appContext));
    }

    public static void setStrictness(Strictness s, Context appContext){
        setSetting("strictness", s.toString(), appContext);
    }

    public static void setCharsetClueType(String charsetId, ClueCard.ClueType clueType, Context appContext){
        setSetting("cluetype_" + charsetId, clueType.toString(), appContext);
    }

    public static ClueCard.ClueType getCharsetClueType(String charsetId, Context appContext){
        try {
            return ClueCard.ClueType.valueOf(getSetting("cluetype_" + charsetId, ClueCard.ClueType.MEANING.toString(), appContext));
        } catch(Throwable t){
            UncaughtExceptionLogger.backgroundLogError("Error parsing clue type for charset", t);
            return ClueCard.ClueType.MEANING;
        }
    }

    public static String getStorySharing(Context appContext){
        return getSetting("story_sharing", null, appContext);

    }

    public static void setStorySharing(String value, Context appContext){
        setSetting("story_sharing", value, appContext);
    }


    // -----------------------

    public static void setBooleanSetting(Context appContext, String name, Boolean value){
        setSetting(name, value == null ? null : Boolean.toString(value), appContext);
    }

    public static Boolean getBooleanSetting(Context appContext, String name, Boolean def){
        String s = getSetting(name, def == null ? null : Boolean.toString(def), appContext);
        if("clear".equals(s)){
            return null;
        }
        if(s != null){
            return Boolean.parseBoolean(s);
        }
        return null;
    }

    public static String getSetting(String key, String defaultValue, Context appContext){
        WriteJapaneseOpenHelper dbh = new WriteJapaneseOpenHelper(appContext);
        try {
            SQLiteDatabase db = dbh.getReadableDatabase();
            Map<String, String> v = DataHelper.selectRecord(
                    db,
                    "SELECT value FROM settings_log WHERE setting = ? ORDER BY timestamp DESC LIMIT 1",
                    key);
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
                    UUID.randomUUID().toString(), Iid.get(appContext).toString(), key, value);
        } finally {
            dbh.close();
        }
    }
}
