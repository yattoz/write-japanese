package dmeeuwis.nakama.teaching;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import dmeeuwis.kanjimaster.R;
import dmeeuwis.nakama.data.DictionarySet;
import dmeeuwis.nakama.kanjidraw.CurveDrawing;
import dmeeuwis.nakama.views.AdvancedFuriganaTextView;
import dmeeuwis.nakama.views.AnimatedCurveView;
import dmeeuwis.nakama.views.KanjiTranslationListAsyncTask;
import dmeeuwis.nakama.views.KanjiTranslationListAsyncTask.AddTranslation;
import dmeeuwis.util.Util;

public class TeachingInfoFragment extends Fragment {


	AnimatedCurveView kanim;
	LinearLayout examplesLayout;
	List<Translation> usedInTranslations;
	TextView kanjiLabel;

	private KanjiTranslationListAsyncTask searchTask;

    @Override
    public void onAttach(Activity activity) {
        Log.i("nakama", "TeachingInfoFragment lifecycle: onAttach");
        super.onAttach(activity);
    }


    @Override
    public void onResume(){
        View view = getView();
        Log.i("nakama", "TeachingInfoFragment lifecycle: onResume; getView=" + view + ", id=" + System.identityHashCode(this));
        final TeachingActivity parent = (TeachingActivity)getActivity();

        Kanji kanji = parent.getKanji();
        String[] currentCharacterSvg = parent.getCurrentCharacterSvg();
        final DictionarySet dictSet = parent.dictSet;

        this.kanim = (AnimatedCurveView)view.findViewById(R.id.kanji_animation);
        this.kanjiLabel = (TextView)view.findViewById(R.id.bigkanji);

        this.usedInTranslations = new LinkedList<>();
        this.examplesLayout = (LinearLayout)view.findViewById(R.id.exampleSpace);

        // TODO: this should probably be made into a RecyclerView
        AddTranslation adder = new AddTranslation(){
            public void add(Translation t){
                Log.i("nakama", "Inflating translation View for: " + t.toString());
                View newTranslation = View.inflate(parent, R.layout.translation_slide, null);
                AdvancedFuriganaTextView af = (AdvancedFuriganaTextView) newTranslation.findViewById(R.id.kanji);
                af.setTranslation(t, dictSet.kanjiFinder());
                TextView eng = (TextView) newTranslation.findViewById(R.id.english);
                eng.setText(t.toEnglishString());
                examplesLayout.addView(newTranslation);
            }
        };

        this.searchTask = new KanjiTranslationListAsyncTask(adder, dictSet, kanji.kanji);
        this.searchTask.execute();

        CurveDrawing animCurveDrawing = new CurveDrawing(currentCharacterSvg);
        kanjiLabel.setText(Character.toString(kanji.kanji));
        this.kanim.setDrawing(animCurveDrawing, AnimatedCurveView.DrawTime.ANIMATED);

        addTextViewsToLayout((LinearLayout)view.findViewById(R.id.meanings), kanji.meanings);

        startAnimation();
        Log.i("nakama", "TeachingInfoFragment lifecycle: at end of onResume, kanim is " + this.kanim + "; kanjiLabel is " + this.kanjiLabel);

        super.onResume();
    }

    public void clear(){
        this.kanim.clear();
    }

    @Override public void onDetach(){
        if(this.searchTask != null) {
            this.searchTask.cancel(true);
        }
        super.onDetach();
    }

    public void startAnimation(){
        if(this.kanim != null) {
            Log.e("nakama", "TeachingInfoFragment lifecycle: startAnimation success. " + System.identityHashCode(this));
            this.kanim.startAnimation(500);
        } else {
            Log.e("nakama", "TeachingInfoFragment lifecycle: can't startAnimation, null reference to kanjim; " + System.identityHashCode(this));
        }
    }
	
	 @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i("nakama", "TeachingInfoFragment lifecycle: onCreateView");
		return inflater.inflate(R.layout.activity_teaching, container, false);
	}
	 
		
	public static void addTextViewsToLayout(LinearLayout l, String[] texts){
		if(texts.length == 0){
			l.setVisibility(View.GONE);
		} else {
			l.setVisibility(View.VISIBLE);
			String joined = Util.join("   ", texts);
			TextView on = new TextView(l.getContext());
			on.setText(joined);
			on.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			Log.d("nakama", "Set a text view: " + joined);
			l.addView(on, params);
		}
	}
}
