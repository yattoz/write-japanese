package dmeeuwis.kanjimaster.logic.data;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.List;

import dmeeuwis.kanjimaster.logic.core.Kanji;
import dmeeuwis.kanjimaster.ui.views.KanjiWithMeaningView;

public class RadicalAdapter extends ArrayAdapter<Kanji> {
    public RadicalAdapter(Context context, int resource, int textViewResourceId, List<Kanji> objects) {
        super(context, resource, textViewResourceId, objects);
    }

    public View getView(int position, View convertView, ViewGroup parentViewgroup){
        //Log.i("nakama", "RadicalAdapter.getView " + convertView);
        if(convertView == null){
            convertView = new KanjiWithMeaningView(this.getContext());
        }
        Kanji k = getItem(position);
        String meaning;
        if(k.meanings.length == 0){
            UncaughtExceptionLogger.backgroundLogError("Error: cannot find meanings for kanji: " + k.toString(), new RuntimeException(), getContext());
            meaning = "";
        } else {
            meaning = k.meanings[0];
        }
        ((KanjiWithMeaningView)convertView).setKanjiAndMeaning(String.valueOf(k.kanji), meaning);
        return convertView;
    }

}

