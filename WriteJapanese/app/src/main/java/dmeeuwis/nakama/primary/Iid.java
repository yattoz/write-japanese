package dmeeuwis.nakama.primary;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.UUID;

public class Iid {

    private static UUID cachedIid;

    /**
     * Returns an install id, creating it and putting it into SharedPreferences if necessary.
     * Not thread-safe, call from ui thread.
     */
    public static UUID get(Application app){
        if(cachedIid != null){
            return cachedIid;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(app);
        String existingInstallId = prefs.getString("iid", null);
        UUID iid = null;
        try {
            iid = UUID.fromString(existingInstallId);
        } catch(Throwable t){
            Log.e("nakama", "Error parsing iid; ignoring.", t);
        }
        if(iid == null) {
            SharedPreferences.Editor ed = prefs.edit();
            iid = UUID.randomUUID();
            Log.i("nakama", "KanjiMasterActivity: setting installId " + iid);
            ed.putString("iid", iid.toString());
            ed.apply();
        }
        cachedIid = iid;
        return iid;
    }
}
