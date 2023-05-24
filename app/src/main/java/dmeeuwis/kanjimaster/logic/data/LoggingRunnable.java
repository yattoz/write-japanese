package dmeeuwis.kanjimaster.logic.data;

public abstract class LoggingRunnable implements Runnable {

    @Override
    public void run() {
        try {
            runCore();
        } catch (Throwable t){
            UncaughtExceptionLogger.backgroundLogError(t.getMessage(), t);
        }
    }

    public abstract void runCore();
}
