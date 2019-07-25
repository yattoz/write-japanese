package dmeeuwis.kanjimaster.ui;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;

import com.jakewharton.threetenabp.AndroidThreeTen;

import dmeeuwis.kanjimaster.logic.data.DataHelper;
import dmeeuwis.kanjimaster.ui.data.DataHelperAndroid;
import dmeeuwis.kanjimaster.logic.data.DataHelperFactory;
import dmeeuwis.kanjimaster.logic.data.ProcessLogRowFactory;
import dmeeuwis.kanjimaster.logic.data.Settings;
import dmeeuwis.kanjimaster.logic.drawing.Constants;
import dmeeuwis.kanjimaster.ui.data.ProcessLogRowAndroid;
import dmeeuwis.kanjimaster.ui.sections.primary.IidAndroid;
import dmeeuwis.kanjimaster.logic.data.IidFactory;

public class KanjiMasterApplicaton extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AndroidThreeTen.init(this);

        Resources r = getResources();
        Constants.MIN_POINT_DISTANCE_PX = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, Constants.MIN_POINT_DISTANCE_DP, r.getDisplayMetrics());
        Constants.MIN_POINT_DISTANCE_FOR_DIRECTION_PX = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, Constants.MIN_POINT_DISTANCE_FOR_DIRECTION_DP, r.getDisplayMetrics());

        Context appContext = getApplicationContext();

        DataHelper dh = new DataHelperAndroid(appContext);
        DataHelperFactory.initialize(dh);

        DataHelper.ProcessRow processor = new ProcessLogRowAndroid(appContext);
        ProcessLogRowFactory.initialize(processor);

        Settings.initialize(appContext);

        IidFactory.initialize(new IidAndroid(appContext));
    }
}
