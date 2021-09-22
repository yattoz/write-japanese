package dmeeuwis.kanjimaster.ui;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.os.StrictMode;
import android.util.TypedValue;

import com.jakewharton.threetenabp.AndroidThreeTen;

import dmeeuwis.kanjimaster.logic.data.DataHelper;
import dmeeuwis.kanjimaster.ui.data.DataHelperAndroid;
import dmeeuwis.kanjimaster.logic.data.DataHelperFactory;
import dmeeuwis.kanjimaster.logic.data.ProcessLogRowFactory;
import dmeeuwis.kanjimaster.logic.data.SettingsFactory;
import dmeeuwis.kanjimaster.logic.drawing.Constants;
import dmeeuwis.kanjimaster.ui.data.ProcessLogRowAndroid;
import dmeeuwis.kanjimaster.ui.data.SettingsAndroid;
import dmeeuwis.kanjimaster.ui.sections.primary.IidAndroid;
import dmeeuwis.kanjimaster.logic.data.IidFactory;
import dmeeuwis.kanjimaster.ui.util.KanjiMasterUncaughtExceptionHandler;

public class KanjiMasterApplicaton extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AndroidThreeTen.init(this);

        Context appContext = getApplicationContext();

        SettingsFactory.initialize(new SettingsAndroid(appContext));

        if (SettingsFactory.get().debug() && false) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                 .detectDiskReads()
                 .detectDiskWrites()
                 .detectNetwork()   // or .detectAll() for all detectable problems
                 .penaltyLog()
                 .build());
         StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                 .detectLeakedSqlLiteObjects()
                 .detectLeakedClosableObjects()
                 .penaltyLog()
                 .penaltyDeath()
                 .build());
        }

        Thread.setDefaultUncaughtExceptionHandler(new KanjiMasterUncaughtExceptionHandler());

        Resources r = getResources();
        Constants.MIN_POINT_DISTANCE_PX = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, Constants.MIN_POINT_DISTANCE_DP, r.getDisplayMetrics());
        Constants.MIN_POINT_DISTANCE_FOR_DIRECTION_PX = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, Constants.MIN_POINT_DISTANCE_FOR_DIRECTION_DP, r.getDisplayMetrics());

        DataHelper dh = new DataHelperAndroid(appContext);
        DataHelperFactory.initialize(dh);

        DataHelper.ProcessRow processor = new ProcessLogRowAndroid(appContext);
        ProcessLogRowFactory.initialize(processor);

        IidFactory.initialize(new IidAndroid(appContext));
    }
}
