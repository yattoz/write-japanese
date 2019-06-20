package dmeeuwis.kanjimaster.ui;

import android.app.Application;

import com.jakewharton.threetenabp.AndroidThreeTen;

import dmeeuwis.kanjimaster.logic.data.UncaughtExceptionLogger;

public class KanjiMasterApplicaton extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AndroidThreeTen.init(this);
        UncaughtExceptionLogger.init(this);
    }
}
