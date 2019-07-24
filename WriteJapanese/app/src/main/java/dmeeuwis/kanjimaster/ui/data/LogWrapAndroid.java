package dmeeuwis.kanjimaster.ui.data;

import android.util.Log;

import dmeeuwis.kanjimaster.logic.data.LogWrap;

public class LogWrapAndroid implements LogWrap {
    private final String tag;

    public LogWrapAndroid(){
        this.tag = "nakama";
    }

    public LogWrapAndroid(String tag) {
        this.tag = tag;
    }

    @Override
    public void i(String message) {
        Log.i(tag, message);
    }

    @Override
    public void i(String message, Throwable t) {
        Log.i(tag, message, t);
    }

    @Override
    public void d(String message) {
        Log.d(tag, message);
    }

    @Override
    public void d(String message, Throwable t) {
        Log.d(tag, message, t);
    }

    @Override
    public void w(String message) {
        Log.w(tag, message);
    }

    @Override
    public void w(String message, Throwable t) {
        Log.w(tag, message, t);
    }

    @Override
    public void e(String message) {
        Log.e(tag, message);
    }

    @Override
    public void e(String message, Throwable t) {
        Log.e(tag, message, t);
    }
}
