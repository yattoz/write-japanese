package dmeeuwis.kanjimaster.logic.data;


import android.app.Activity;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import dmeeuwis.kanjimaster.logic.core.Kanji;
import dmeeuwis.kanjimaster.logic.core.KanjiRadicalFinder;

public class LoadRadicalsFile extends AsyncTask<Void, Void, List<Kanji>> {
    final Activity parent;
    final char character;
    final ArrayAdapter<Kanji> radicalAdapter;
    final View radicalsCard;

    public LoadRadicalsFile(Activity parent, char character, ArrayAdapter<Kanji> adapter, View card){
        this.parent = parent;
        this.character = character;
        this.radicalAdapter = adapter;
        this.radicalsCard = card;
    }

    @Override
    protected List<Kanji> doInBackground(Void... v) {
        Thread.currentThread().setName("LoadRadicalsFile");
        DictionarySet dicts = DictionarySet.get(parent);
        List<Kanji> retRadicals = null;
        try {

            AssetManager asm = parent.getAssets();
            InputStream kif = asm.open("kradfile");
            try {
                KanjiRadicalFinder krf = new KanjiRadicalFinder(kif);
                retRadicals = krf.findRadicalsAsKanji(dicts.kanjiFinder(), character);
            } finally {
                kif.close();
            }
        } catch (IOException e) {
            Log.e("nakama", "Error: could not read kradfile entries to kanji.", e);
            retRadicals = new ArrayList<Kanji>(0);
        }

        return retRadicals;
    }

    @Override
    protected void onPostExecute(List<Kanji> result) {
        if(result.size() > 0){
            for(Kanji k: result){
                //Log.i("nakama", "Adding results to radicalAdapter: " + k);
                radicalAdapter.add(k);
            }
            radicalsCard.setVisibility(View.VISIBLE);
            radicalAdapter.notifyDataSetChanged();
        }
    }
}
