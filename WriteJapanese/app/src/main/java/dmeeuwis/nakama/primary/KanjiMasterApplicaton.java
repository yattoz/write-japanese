package dmeeuwis.nakama.primary;

import android.app.Application;

import com.jakewharton.threetenabp.AndroidThreeTen;

public class KanjiMasterApplicaton extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AndroidThreeTen.init(this);
    }
}
