package dmeeuwis.kanjimaster.logic.data;

import java.net.MalformedURLException;
import java.net.URL;

public class HostFinder {
    private static boolean DEBUG_NETWORK = false;

    public static URL formatURL(String url){
        try {
            return new URL(hostPrefix() + url);
        } catch(MalformedURLException e){
            throw new RuntimeException(e);
        }
    }

    public static String hostPrefix(){
        if(SettingsFactory.get().debug() && DEBUG_NETWORK){
            if(isEmulator()) {
                return "http://10.0.2.2:8080";
            }
            return "http://192.168.1.52:8080";
        }
        return "https://dmeeuwis.com";
    }

    private static boolean isEmulator() {
        return SettingsFactory.get().isEmulator();
    }
}
