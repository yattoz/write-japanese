package dmeeuwis.kanjimaster.ui.sections.teaching;

import android.content.res.Resources;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import dmeeuwis.kanjimaster.core.Kanji;
import dmeeuwis.kanjimaster.core.Translation;
import dmeeuwis.kanjimaster.core.indexer.KanjiFinder;
import dmeeuwis.kanjimaster.R;
import dmeeuwis.kanjimaster.logic.data.DictionarySet;
import dmeeuwis.kanjimaster.logic.drawing.CurveDrawing;
import dmeeuwis.kanjimaster.ui.data.DictionarySetAndroid;
import dmeeuwis.kanjimaster.ui.views.translations.CharacterTranslationListAsyncTask;
import dmeeuwis.kanjimaster.ui.views.translations.CharacterTranslationListAsyncTask.AddTranslation;
import dmeeuwis.kanjimaster.ui.views.translations.KanjiVocabRecyclerAdapter;

public class TeachingInfoFragment extends Fragment {

    float engTextSize;

	private CharacterTranslationListAsyncTask searchTask;

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

        DictionarySet d = DictionarySetAndroid.get(getContext());
        KanjiFinder kf = d.kanjiFinder();

        RecyclerView rec = (RecyclerView)view.findViewById(R.id.teaching_recycler);
        rec.setLayoutManager(new LinearLayoutManager(getActivity()));
        final KanjiVocabRecyclerAdapter adapt = new KanjiVocabRecyclerAdapter(getActivity(), kf);
        adapt.addCharacterHeader(String.valueOf(character), new CurveDrawing(currentCharacterSvg));
        if(kanji != null) {
            adapt.addMeaningsHeader(kanji.toMeaningString());
            adapt.addReadingsHeader(kanji.kanji);
        }
        rec.setAdapter(adapt);

        // TODO: this should probably be made into a RecyclerView
        AddTranslation adder = new AddTranslation(){
            public void add(Translation t){
                Log.i("nakama", "Adding translation to list " + t.toString());
                adapt.add(t);
            }
        };

        this.searchTask = new CharacterTranslationListAsyncTask(adder, getActivity().getApplicationContext(), character);
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
