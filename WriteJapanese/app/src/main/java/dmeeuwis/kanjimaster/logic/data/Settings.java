package dmeeuwis.kanjimaster.logic.data;

import dmeeuwis.kanjimaster.ui.views.translations.ClueCard;

public interface Settings {
    Boolean getSRSEnabled();

    Boolean getSRSNotifications();

    Boolean getSRSAcrossSets();

    void setCrossDeviceSyncAsked();

    void clearCrossDeviceSync();

    void clearSRSSettings();

    void setInstallDate();

    Object getInstallDate();

    String osVersion();

    SyncStatus getCrossDeviceSyncEnabled();

    Strictness getStrictness();

    void setStrictness(Strictness s);

    void setCharsetClueType(String charsetId, ClueCard.ClueType clueType);

    ClueCard.ClueType getCharsetClueType(String charsetId);

    String getStorySharing();

    void setStorySharing(String value);

    void setBooleanSetting(String name, Boolean value);

    Boolean getBooleanSetting(String name, Boolean def);

    String getSetting(String key, String defaultValue);

    void setSetting(String key, String value);

    void deleteSetting(String key);

    CharacterProgressDataHelper.ProgressionSettings getProgressionSettings();

    void clearSyncSettingsDebug();

    SyncSettings getSyncSettings();

    void setSyncSettings(SyncSettings set);

    int version();

    boolean debug();

    String device();

    enum Strictness {CASUAL, CASUAL_ORDERED, STRICT}

    class SyncSettings {

        public String lastSyncServerTimestamp;
        public String lastSyncDeviceTimestamp;

        public SyncSettings(String serverTimestamp, String deviceTimestamp){
            this.lastSyncServerTimestamp = serverTimestamp;
            this.lastSyncDeviceTimestamp = deviceTimestamp;
        }
    }

    class SyncStatus {
        public final boolean asked;
        public final String authcode;

        public SyncStatus(boolean asked, String authcode){
            this.asked = asked;
            this.authcode = authcode;
        }

        public String toString(){
            return String.format("[SyncStatus asked=%b authcode=%s]", asked, authcode);
        }
    }
}
