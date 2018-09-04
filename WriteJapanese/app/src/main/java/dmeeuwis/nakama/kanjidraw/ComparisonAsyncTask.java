package dmeeuwis.nakama.kanjidraw;

import android.content.Context;
import android.database.sqlite.SQLiteFullException;
import android.os.AsyncTask;
import android.widget.Toast;

import java.io.IOException;

import dmeeuwis.nakama.data.CharacterStudySet;
import dmeeuwis.nakama.data.ProgressTracker;
import dmeeuwis.nakama.data.UncaughtExceptionLogger;
import dmeeuwis.nakama.primary.*;

public class ComparisonAsyncTask extends AsyncTask<Void, Void, Criticism> {

    public interface OnCriticismDone {
        void run(Criticism c, CharacterStudySet.GradingResult entry);
    }

    private KanjiMasterActivity appContext;
    private Character currChar;
    private String setId;
    private Comparator comparator;
    private PointDrawing drawn;
    private CurveDrawing known;
    private OnCriticismDone onDone;

    public ComparisonAsyncTask(KanjiMasterActivity appContext, Character currChar, String setId, Comparator comparator, PointDrawing drawn, CurveDrawing known, OnCriticismDone onDone){
        this.appContext = appContext;
        this.comparator = comparator;
        this.currChar = currChar;
        this.setId = setId;
        this.drawn = drawn;
        this.known = known;
        this.onDone = onDone;
    }

    @Override
    protected Criticism doInBackground(Void ... params) {
        try {
            return comparator.compare(currChar, drawn, known);
        } catch (IOException e) {
            UncaughtExceptionLogger.backgroundLogError("IO error from comparator", e, appContext);
            return null;
        }
    }

    @Override
    protected void onPostExecute(Criticism criticism) {
        CharacterStudySet.GradingResult entry = null;
        try {
            entry = appContext.mark(currChar, setId, drawn, criticism.pass);
        } catch(SQLiteFullException e){
            Toast.makeText(appContext, "Could not record progress: disk is full.", Toast.LENGTH_SHORT).show();
            UncaughtExceptionLogger.backgroundLogError("Could not record progress: disk is full", e, appContext);
        }

        onDone.run(criticism, entry);
    }
}