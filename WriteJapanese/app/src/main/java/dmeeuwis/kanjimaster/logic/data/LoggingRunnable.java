package dmeeuwis.kanjimaster.logic.data;

import android.content.Context;

import dmeeuwis.kanjimaster.ui.data.UncaughtExceptionLogger;

public abstract class LoggingRunnable implements Runnable {

    final Context context;

    public LoggingRunnable(Context context){
        this.context = context;
    }

    @Override
    public void run() {
        try {
            runCore();
        } catch (Throwable t){
            UncaughtExceptionLogger.backgroundLogError(t.getMessage(), t, context);
        }
    }

    public abstract void runCore();
}
