package dmeeuwis.kanjimaster.logic.data;

public class SettingsFactory {

    static Settings settings;

    public static void initialize(Settings s){
        settings = s;
    }

    public static Settings get() {
        return settings;
    }

}
