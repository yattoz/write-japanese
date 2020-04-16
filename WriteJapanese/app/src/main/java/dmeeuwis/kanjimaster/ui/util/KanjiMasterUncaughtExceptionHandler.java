package dmeeuwis.kanjimaster.ui.util;

import android.os.StrictMode;

import dmeeuwis.kanjimaster.logic.data.UncaughtExceptionLogger;

public class KanjiMasterUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
    @Override
    public void uncaughtException(final Thread thread, final Throwable ex) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        UncaughtExceptionLogger.logError(thread, "Uncaught top level error: ", ex);
    }
}
