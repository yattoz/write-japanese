package dmeeuwis.kanjimaster.logic.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.threeten.bp.LocalDateTime;

import java.util.Map;
import java.util.UUID;

import dmeeuwis.kanjimaster.ui.sections.primary.IidFactory;
import dmeeuwis.kanjimaster.ui.sections.primary.IntroActivity;
import dmeeuwis.kanjimaster.ui.sections.primary.ProgressSettingsDialog;
import dmeeuwis.kanjimaster.ui.views.translations.ClueCard;

public class Settings {
    public static final String INSTALL_TIME_PREF_NAME = "INSTALL_TIME";

    public static Context appContext;

    public static void initialize(Context ctx){
       appContext = ctx;
    }

    public static Boolean getSRSEnabled() {
        return getBooleanSetting(IntroActivity.USE_SRS_SETTING_NAME, null);
    }

    public static Boolean getSRSNotifications() {
        return getBooleanSetting(IntroActivity.SRS_NOTIFICATION_SETTING_NAME, null);
    }

    public static Boolean getSRSAcrossSets() {
        return getBooleanSetting(IntroActivity.SRS_ACROSS_SETS, true);
    }

    public static void setCrossDeviceSyncAsked() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        SharedPreferences.Editor ed = prefs.edit();
        ed.putBoolean(SyncRegistration.HAVE_ASKED_ABOUT_SYNC_KEY, true);
        ed.apply();
    }


    public static void clearCrossDeviceSync() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        SharedPreferences.Editor ed = prefs.edit();
        ed.remove(SyncRegistration.HAVE_ASKED_ABOUT_SYNC_KEY);
        ed.remove(SyncRegistration.AUTHCODE_SHARED_PREF_KEY);
        ed.apply();
    }


    public static void clearSRSSettings() {
        Settings.setSetting(IntroActivity.USE_SRS_SETTING_NAME, "clear");
        Settings.setSetting(IntroActivity.SRS_ACROSS_SETS, "clear");
    }

    public static void setInstallDate(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        String installDate = prefs.getString(INSTALL_TIME_PREF_NAME, null);
        if(installDate == null) {
            Map<String, String> v = DataHelperFactory.get().selectRecord(
                    "SELECT min(timestamp) as min FROM practice_log");
            String time = v.get("min") ;
            if(time == null){
                time = LocalDateTime.now().toString();
            }
            SharedPreferences.Editor ed = prefs.edit();
            ed.putString(INSTALL_TIME_PREF_NAME, time);
            ed.apply();
        }
    }

    public static Object getInstallDate() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        return prefs.getString(INSTALL_TIME_PREF_NAME, null);
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

    public static SyncStatus getCrossDeviceSyncEnabled() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        Boolean asked = prefs.getBoolean(SyncRegistration.HAVE_ASKED_ABOUT_SYNC_KEY, false);
        String authcode = prefs.getString(SyncRegistration.AUTHCODE_SHARED_PREF_KEY, null);
        return new SyncStatus(asked, authcode);
    }

    public enum Strictness { CASUAL, CASUAL_ORDERED, STRICT }

    public static Strictness getStrictness(){
        try {
            return Strictness.valueOf(getSetting("strictness", Strictness.CASUAL.toString()));
        } catch(IllegalArgumentException e){
            return Strictness.CASUAL_ORDERED;
        }
    }

    public static void setStrictness(Strictness s){
        setSetting("strictness", s.toString());
    }

    public static void setCharsetClueType(String charsetId, ClueCard.ClueType clueType){
        setSetting("cluetype_" + charsetId, clueType.toString());
    }

    public static ClueCard.ClueType getCharsetClueType(String charsetId){
        try {
            return ClueCard.ClueType.valueOf(getSetting("cluetype_" + charsetId, ClueCard.ClueType.MEANING.toString()));
        } catch(Throwable t){
            UncaughtExceptionLogger.backgroundLogError("Error parsing clue type for charset", t);
            return ClueCard.ClueType.MEANING;
        }
    }

    public static String getStorySharing(){
        return getSetting("story_sharing", null);

    }

    public static void setStorySharing(String value){
        setSetting("story_sharing", value);
    }


    // -----------------------

    public static void setBooleanSetting(String name, Boolean value){
        setSetting(name, value == null ? null : Boolean.toString(value));
    }

    public static Boolean getBooleanSetting(String name, Boolean def){
        String s = getSetting(name, def == null ? null : Boolean.toString(def));
        if("clear".equals(s)){
            return null;
        }
        if(s != null){
            return Boolean.parseBoolean(s);
        }
        return null;
    }

    public static String getSetting(String key, String defaultValue){
        Map<String, String> v = DataHelperFactory.get().selectRecord(
                "SELECT value FROM settings_log WHERE setting = ? ORDER BY timestamp DESC LIMIT 1",
                key);
        if(v == null){
            return defaultValue;
        }
        return v.get("value");
    }

    public static void setSetting(String key, String value){
        DataHelperFactory.get().execSQL(
                "INSERT INTO settings_log(id, install_id, timestamp, setting, value) VALUES(?, ?, CURRENT_TIMESTAMP, ?, ?)",
                new String[] { UUID.randomUUID().toString(), IidFactory.get().toString(), key, value });
    }

    public static void deleteSetting(String key){
        DataHelperFactory.get().execSQL(
                "DELETE FROM settings_log WHERE setting = ?",
                new String[] { key });
    }

    public static CharacterProgressDataHelper.ProgressionSettings getProgressionSettings(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        return new CharacterProgressDataHelper.ProgressionSettings(
                prefs.getInt(ProgressSettingsDialog.SHARED_PREFS_KEY_INTRO_INCORRECT, CharacterProgressDataHelper.DEFAULT_INTRO_INCORRECT),
                prefs.getInt(ProgressSettingsDialog.SHARED_PREFS_KEY_INTRO_REVIEWING, CharacterProgressDataHelper.DEFAULT_INTRO_REVIEWING),
                prefs.getInt(ProgressSettingsDialog.SHARED_PREFS_KEY_ADV_INCORRECT, CharacterProgressDataHelper.DEFAULT_ADV_INCORRECT),
                prefs.getInt(ProgressSettingsDialog.SHARED_PREFS_KEY_ADV_REVIEWING, CharacterProgressDataHelper.DEFAULT_ADV_REVIEWING),
                prefs.getInt(ProgressSettingsDialog.SHARED_PREFS_KEY_CHAR_COOLDOWN, CharacterProgressDataHelper.DEFAULT_CHAR_COOLDOWN),
                prefs.getBoolean(ProgressSettingsDialog.SHARED_PREFS_KEY_SKIP_SRS_ON_FIRST_CORRECT, CharacterProgressDataHelper.DEFAULT_SKIP_SRS_ON_FIRST_CORRECT));
    }
}
