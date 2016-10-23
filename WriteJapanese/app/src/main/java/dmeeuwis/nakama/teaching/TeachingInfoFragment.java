package dmeeuwis.nakama.teaching;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;

import dmeeuwis.Kanji;
import dmeeuwis.Translation;
import dmeeuwis.indexer.KanjiFinder;
import dmeeuwis.kanjimaster.R;
import dmeeuwis.nakama.data.DictionarySet;
import dmeeuwis.nakama.kanjidraw.Criticism;
import dmeeuwis.nakama.kanjidraw.CurveDrawing;
import dmeeuwis.nakama.views.AdvancedFuriganaTextView;
import dmeeuwis.nakama.views.AnimatedCurveView;
import dmeeuwis.nakama.views.translations.KanjiTranslationListAsyncTask;
import dmeeuwis.nakama.views.translations.KanjiTranslationListAsyncTask.AddTranslation;
import dmeeuwis.nakama.views.translations.KanjiVocabRecyclerAdapter;
import dmeeuwis.util.Util;
import uk.co.deanwild.flowtextview.FlowTextView;

public class TeachingInfoFragment extends Fragment {

    float engTextSize;

	private KanjiTranslationListAsyncTask searchTask;

    @Override
    public void onResume(){
        View view = getView();
        //Log.i("nakama", "TeachingInfoFragment lifecycle: onResume; getView=" + view + ", id=" + System.identityHashCode(this));
        final TeachingActivity parent = (TeachingActivity)getActivity();

        Kanji kanji = parent.getKanji();
        char character = kanji == null ? parent.getCharacter().charAt(0) : kanji.kanji;

        String[] currentCharacterSvg = parent.getCurrentCharacterSvg();
        final DictionarySet dictSet = parent.dictSet;

        Resources r = getActivity().getResources();
        this.engTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, r.getDisplayMetrics());

        DictionarySet d = DictionarySet.get(getContext());
        KanjiFinder kf = d.kanjiFinder();

        RecyclerView rec = (RecyclerView)view.findViewById(R.id.teaching_recycler);
        rec.setLayoutManager(new LinearLayoutManager(getActivity()));
        final KanjiVocabRecyclerAdapter adapt = new KanjiVocabRecyclerAdapter(getActivity(), kf);
        adapt.addCharacterHeader(String.valueOf(kanji.kanji), new CurveDrawing(currentCharacterSvg));
        adapt.addMeaningsHeader(kanji.toMeaningString());
        rec.setAdapter(adapt);

        // TODO: this should probably be made into a RecyclerView
        AddTranslation adder = new AddTranslation(){
            public void add(Translation t){
                Log.i("nakama", "Adding translation to list " + t.toString());
                adapt.add(t);
            }
        };

        this.searchTask = new KanjiTranslationListAsyncTask(adder, dictSet, character);
        this.searchTask.execute();

        startAnimation();
        //Log.i("nakama", "TeachingInfoFragment lifecycle: at end of onResume, kanim is " + this.kanim + "; kanjiLabel is " + this.kanjiLabel);

        super.onResume();
    }

    public void clear(){
        // this.kanim.clear();
    }

    @Override public void onDetach(){
        if(this.searchTask != null) {
            this.searchTask.cancel(true);
        }
        super.onDetach();
    }

    public void startAnimation(){
        //if(this.kanim != null) {
            //Log.d("nakama", "TeachingInfoFragment lifecycle: startAnimation success. " + System.identityHashCode(this));
        //this.kanim.startAnimation(500);
        //}
    }
	
	 @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //Log.d("nakama", "TeachingInfoFragment lifecycle: onCreateView");
		return inflater.inflate(R.layout.activity_teaching, container, false);
	}
}
