package dmeeuwis.kanjimaster.logic.data;

public interface Settings {
    Boolean getSRSEnabled();

    Boolean getSRSNotifications();

    Boolean getSRSAcrossSets();

    void clearSRSSettings();

    void setInstallDate();

    Object getInstallDate();

    String osVersion();

    Strictness getStrictness();

    void setStrictness(Strictness s);

    void setCharsetClueType(String charsetId, ClueType clueType);

    ClueType getCharsetClueType(String charsetId);

    String getStorySharing();

    void setStorySharing(String value);

    void setBooleanSetting(String name, Boolean value);

    Boolean getBooleanSetting(String name, Boolean def);

    String getSetting(String key, String defaultValue);

    void setSetting(String key, String value);

    void deleteSetting(String key);

    CharacterProgressDataHelper.ProgressionSettings getProgressionSettings();

    int version();

    boolean debug();

    String device();

    boolean isEmulator();

    enum Strictness {CASUAL, CASUAL_ORDERED, STRICT}


    /* Methods related to settings sync.
     * TODO: BACKUP FEATURE: sort this out
     */
    
    void clearSyncSettingsDebug();

    SyncSettings getSyncSettings();

    void setSyncSettings(SyncSettings set);

    void setCrossDeviceSyncAsked();

    void clearCrossDeviceSync();


    class SyncSettings {

        public String lastSyncServerTimestamp;
        public String lastSyncDeviceTimestamp;

        public SyncSettings(String serverTimestamp, String deviceTimestamp){
            this.lastSyncServerTimestamp = serverTimestamp;
            this.lastSyncDeviceTimestamp = deviceTimestamp;
        }

        @Override public String toString(){
            return String.format("[Sync lastDevice=" + lastSyncDeviceTimestamp + " lastService=" + lastSyncServerTimestamp + "]");
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
