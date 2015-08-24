package dmeeuwis.nakama.data;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.List;

import dmeeuwis.Kanji;
import dmeeuwis.nakama.views.KanjiWithMeaningView;

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
        ((KanjiWithMeaningView)convertView).setKanjiAndMeaning(String.valueOf(k.kanji), k.meanings[0]);
        return convertView;
    }

}

