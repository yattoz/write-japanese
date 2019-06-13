package dmeeuwis.nakama.data;

import android.os.Build;

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
            if(isEmulator()) {
                return "http://10.0.2.2:8080";
            }
            return "http://192.168.1.52:8080";
        }
        return "https://dmeeuwis.com";
    }

    private static boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
            || Build.FINGERPRINT.startsWith("unknown")
            || Build.MODEL.contains("google_sdk")
            || Build.MODEL.contains("Emulator")
            || Build.MODEL.contains("Android SDK built for x86")
            || Build.MANUFACTURER.contains("Genymotion")
            || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
            || "google_sdk".equals(Build.PRODUCT);
    }
}
