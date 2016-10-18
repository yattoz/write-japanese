package dmeeuwis.nakama.kanjidraw;

import android.content.Context;
import android.os.AsyncTask;

import dmeeuwis.nakama.data.CharacterStudySet;

public class ComparisonAsyncTask extends AsyncTask<Void, Void, Criticism> {

    public interface OnCriticismDone {
        void run(Criticism c);
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
        final Criticism critique = comparator.compare(currentCharacterSet.currentCharacter(), drawn, known);
        return critique;
    }


    @Override
    protected void onPostExecute(Criticism criticism) {
        currentCharacterSet.markCurrent(drawn, criticism.pass, appContext);
        onDone.run(criticism);
    }
}
