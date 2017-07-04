package dmeeuwis.nakama.kanjidraw;

import android.content.Context;
import android.database.sqlite.SQLiteFullException;
import android.os.AsyncTask;
import android.widget.Toast;

import java.io.IOException;

import dmeeuwis.nakama.data.CharacterStudySet;
import dmeeuwis.nakama.data.ProgressTracker;
import dmeeuwis.nakama.data.UncaughtExceptionLogger;

public class ComparisonAsyncTask extends AsyncTask<Void, Void, Criticism> {

    public interface OnCriticismDone {
        void run(Criticism c, ProgressTracker.SRSEntry entry);
    }

    private Context appContext;
    private Comparator comparator;
    private CharacterStudySet currentCharacterSet;
    private PointDrawing drawn;
    private CurveDrawing known;
    private OnCriticismDone onDone;

    public ComparisonAsyncTask(Context appContext, Comparator comparator, CharacterStudySet currentCharacterSet, PointDrawing drawn, CurveDrawing known, OnCriticismDone onDone){
        this.appContext = appContext;
        this.comparator = comparator;
        this.currentCharacterSet = currentCharacterSet;
        this.drawn = drawn;
        this.known = known;
        this.onDone = onDone;
    }

    @Override
    protected Criticism doInBackground(Void ... params) {
        try {
            return comparator.compare(currentCharacterSet.currentCharacter(), drawn, known);
        } catch (IOException e) {
            UncaughtExceptionLogger.backgroundLogError("IO error from comparator", e, appContext);
            return null;
        }
    }

    @Override
    protected void onPostExecute(Criticism criticism) {
        ProgressTracker.SRSEntry entry = null;
        try {
            entry = currentCharacterSet.markCurrent(drawn, criticism.pass);
        } catch(SQLiteFullException e){
            Toast.makeText(appContext, "Could not record progress: disk is full.", Toast.LENGTH_SHORT).show();
            UncaughtExceptionLogger.backgroundLogError("Could not record progress: disk is full", e, appContext);
        }

        onDone.run(criticism, entry);
    }
}