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
}
