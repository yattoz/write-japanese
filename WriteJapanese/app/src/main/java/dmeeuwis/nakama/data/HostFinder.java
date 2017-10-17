package dmeeuwis.nakama.data;

import java.net.MalformedURLException;
import java.net.URL;

import dmeeuwis.kanjimaster.BuildConfig;

public class HostFinder {
    private static boolean DEBUG_NETWORK = true;

    public static URL formatURL(String url){
        try {
            return new URL(hostPrefix() + url);
        } catch(MalformedURLException e){
            throw new RuntimeException(e);
        }
    }

    public static String hostPrefix(){
        if(BuildConfig.DEBUG && DEBUG_NETWORK){
            return "http://10.0.2.2:8080";
        }
        return "https://dmeeuwis.com";
    }
}
