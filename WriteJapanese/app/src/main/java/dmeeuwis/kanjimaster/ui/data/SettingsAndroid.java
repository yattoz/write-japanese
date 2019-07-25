package dmeeuwis.kanjimaster.ui.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import org.threeten.bp.LocalDateTime;

import java.util.Map;
import java.util.UUID;

import dmeeuwis.kanjimaster.BuildConfig;
import dmeeuwis.kanjimaster.logic.data.CharacterProgressDataHelper;
import dmeeuwis.kanjimaster.logic.data.DataHelperFactory;
import dmeeuwis.kanjimaster.logic.data.IidFactory;
import dmeeuwis.kanjimaster.logic.data.Settings;
import dmeeuwis.kanjimaster.logic.data.UncaughtExceptionLogger;
import dmeeuwis.kanjimaster.ui.sections.primary.IntroActivity;
import dmeeuwis.kanjimaster.ui.sections.primary.ProgressSettingsDialog;
import dmeeuwis.kanjimaster.ui.views.translations.ClueCard;

public class SettingsAndroid implements Settings {
    public static final String INSTALL_TIME_PREF_NAME = "INSTALL_TIME";
    final public static String SERVER_SYNC_PREFS_KEY = "progress-server-sync-time";
    final public static String DEVICE_SYNC_PREFS_KEY = "progress-device-sync-time";

    public Context appContext;

    public SettingsAndroid(Context ctx) {
        appContext = ctx;
    }

    @Override
    public Boolean getSRSEnabled() {
        return getBooleanSetting(IntroActivity.USE_SRS_SETTING_NAME, null);
    }

    @Override
    public Boolean getSRSNotifications() {
        return getBooleanSetting(IntroActivity.SRS_NOTIFICATION_SETTING_NAME, null);
    }

    @Override
    public Boolean getSRSAcrossSets() {
        return getBooleanSetting(IntroActivity.SRS_ACROSS_SETS, true);
    }

    @Override
    public void setCrossDeviceSyncAsked() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        SharedPreferences.Editor ed = prefs.edit();
        ed.putBoolean(SyncRegistration.HAVE_ASKED_ABOUT_SYNC_KEY, true);
        ed.apply();
    }


    @Override
    public void clearCrossDeviceSync() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        SharedPreferences.Editor ed = prefs.edit();
        ed.remove(SyncRegistration.HAVE_ASKED_ABOUT_SYNC_KEY);
        ed.remove(SyncRegistration.AUTHCODE_SHARED_PREF_KEY);
        ed.apply();
    }


    @Override
    public void clearSRSSettings() {
        setSetting(IntroActivity.USE_SRS_SETTING_NAME, "clear");
        setSetting(IntroActivity.SRS_ACROSS_SETS, "clear");
    }

    @Override
    public void setInstallDate() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        String installDate = prefs.getString(INSTALL_TIME_PREF_NAME, null);
        if (installDate == null) {
            Map<String, String> v = DataHelperFactory.get().selectRecord(
                    "SELECT min(timestamp) as min FROM practice_log");
            String time = v.get("min");
            if (time == null) {
                time = LocalDateTime.now().toString();
            }
            SharedPreferences.Editor ed = prefs.edit();
            ed.putString(INSTALL_TIME_PREF_NAME, time);
            ed.apply();
        }
    }

    @Override
    public Object getInstallDate() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        return prefs.getString(INSTALL_TIME_PREF_NAME, null);
    }

    @Override
    public String osVersion() {
        return Build.VERSION.RELEASE;
    }

    @Override
    public Settings.SyncStatus getCrossDeviceSyncEnabled() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        Boolean asked = prefs.getBoolean(SyncRegistration.HAVE_ASKED_ABOUT_SYNC_KEY, false);
        String authcode = prefs.getString(SyncRegistration.AUTHCODE_SHARED_PREF_KEY, null);
        return new Settings.SyncStatus(asked, authcode);
    }

    @Override
    public Settings.Strictness getStrictness() {
        try {
            return Settings.Strictness.valueOf(getSetting("strictness", Settings.Strictness.CASUAL.toString()));
        } catch (IllegalArgumentException e) {
            return Settings.Strictness.CASUAL_ORDERED;
        }
    }

    @Override
    public void setStrictness(Settings.Strictness s) {
        setSetting("strictness", s.toString());
    }

    @Override
    public void setCharsetClueType(String charsetId, ClueCard.ClueType clueType) {
        setSetting("cluetype_" + charsetId, clueType.toString());
    }

    @Override
    public ClueCard.ClueType getCharsetClueType(String charsetId) {
        try {
            return ClueCard.ClueType.valueOf(getSetting("cluetype_" + charsetId, ClueCard.ClueType.MEANING.toString()));
        } catch (Throwable t) {
            UncaughtExceptionLogger.backgroundLogError("Error parsing clue type for charset", t);
            return ClueCard.ClueType.MEANING;
        }
    }

    @Override
    public String getStorySharing() {
        return getSetting("story_sharing", null);

    }

    @Override
    public void setStorySharing(String value) {
        setSetting("story_sharing", value);
    }


    // -----------------------

    @Override
    public void setBooleanSetting(String name, Boolean value) {
        setSetting(name, value == null ? null : Boolean.toString(value));
    }

    @Override
    public Boolean getBooleanSetting(String name, Boolean def) {
        String s = getSetting(name, def == null ? null : Boolean.toString(def));
        if ("clear".equals(s)) {
            return null;
        }
        if (s != null) {
            return Boolean.parseBoolean(s);
        }
        return null;
    }

    @Override
    public String getSetting(String key, String defaultValue) {
        Map<String, String> v = DataHelperFactory.get().selectRecord(
                "SELECT value FROM settings_log WHERE setting = ? ORDER BY timestamp DESC LIMIT 1",
                key);
        if (v == null) {
            return defaultValue;
        }
        return v.get("value");
    }

    @Override
    public void setSetting(String key, String value) {
        DataHelperFactory.get().execSQL(
                "INSERT INTO settings_log(id, install_id, timestamp, setting, value) VALUES(?, ?, CURRENT_TIMESTAMP, ?, ?)",
                new String[]{UUID.randomUUID().toString(), IidFactory.get().toString(), key, value});
    }

    @Override
    public void deleteSetting(String key) {
        DataHelperFactory.get().execSQL(
                "DELETE FROM settings_log WHERE setting = ?",
                new String[]{key});
    }

    @Override
    public CharacterProgressDataHelper.ProgressionSettings getProgressionSettings() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        return new CharacterProgressDataHelper.ProgressionSettings(
                prefs.getInt(ProgressSettingsDialog.SHARED_PREFS_KEY_INTRO_INCORRECT, CharacterProgressDataHelper.DEFAULT_INTRO_INCORRECT),
                prefs.getInt(ProgressSettingsDialog.SHARED_PREFS_KEY_INTRO_REVIEWING, CharacterProgressDataHelper.DEFAULT_INTRO_REVIEWING),
                prefs.getInt(ProgressSettingsDialog.SHARED_PREFS_KEY_ADV_INCORRECT, CharacterProgressDataHelper.DEFAULT_ADV_INCORRECT),
                prefs.getInt(ProgressSettingsDialog.SHARED_PREFS_KEY_ADV_REVIEWING, CharacterProgressDataHelper.DEFAULT_ADV_REVIEWING),
                prefs.getInt(ProgressSettingsDialog.SHARED_PREFS_KEY_CHAR_COOLDOWN, CharacterProgressDataHelper.DEFAULT_CHAR_COOLDOWN),
                prefs.getBoolean(ProgressSettingsDialog.SHARED_PREFS_KEY_SKIP_SRS_ON_FIRST_CORRECT, CharacterProgressDataHelper.DEFAULT_SKIP_SRS_ON_FIRST_CORRECT));
    }

    @Override
    public void clearSyncSettingsDebug() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        SharedPreferences.Editor e = prefs.edit();
        e.putString(SERVER_SYNC_PREFS_KEY, "2000-01-01 00:00:00 +00");
        e.putString(DEVICE_SYNC_PREFS_KEY, "0");
        e.apply();
    }

    @Override
    public Settings.SyncSettings getSyncSettings() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        String lastSyncServerTimestamp = prefs.getString(SERVER_SYNC_PREFS_KEY, "2000-01-01 00:00:00 +00");
        String lastSyncDeviceTimestamp = prefs.getString(DEVICE_SYNC_PREFS_KEY, "0");
        return new Settings.SyncSettings(lastSyncServerTimestamp, lastSyncDeviceTimestamp);
    }

    @Override
    public void setSyncSettings(Settings.SyncSettings set) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        SharedPreferences.Editor ed = prefs.edit();
        ed.putString(DEVICE_SYNC_PREFS_KEY, set.lastSyncDeviceTimestamp);
        ed.putString(SERVER_SYNC_PREFS_KEY, set.lastSyncServerTimestamp);
        ed.apply();
    }

    @Override
    public int version(){
        return BuildConfig.VERSION_CODE;
    }

    @Override
    public boolean debug() {
        return BuildConfig.DEBUG;
    }

    @Override
    public String device(){
        return Build.MANUFACTURER + ": " + Build.MODEL;
    }

}

