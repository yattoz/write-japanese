package dmeeuwis.masterlibrary; 
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

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
import android.widget.Toast;
import dmeeuwis.Kanji;
import dmeeuwis.Translation;
import dmeeuwis.kanjimaster.R;
import dmeeuwis.masterlibrary.KanjiTranslationListAsyncTask.AddTranslation;
import dmeeuwis.nakama.helpers.DictionarySet;
import dmeeuwis.nakama.kanjidraw.Glyph;
import dmeeuwis.nakama.views.AdvancedFuriganaTextView;
import dmeeuwis.nakama.views.AnimatedCurveView;
import dmeeuwis.util.Util;

public class TeachingInfoFragment extends Fragment {

	Kanji kanji;
	String[] currentCharacterSvg;
	
	AnimatedCurveView kanim;
	LinearLayout examplesLayout;
	List<Translation> usedInTranslations;
	TextView kanjiLabel;
	View view;
	Activity activity;
	
	DictionarySet dictSet;
	
	private KanjiTranslationListAsyncTask searchTask;

	@Override public void onAttach(Activity activity){
		TeachingActivity parent = (TeachingActivity)getActivity();
		Log.d("nakama", "TeachingInfoFragment: parent is " + parent + ", kanji is " + this.kanji);
		this.activity = activity;

        DictionarySet result;
        synchronized (DictionarySet.class) {
            result = new DictionarySet(activity);
        }
        DictionarySet sd = result;
		try {
			this.kanji = sd.kanjiFinder().find(parent.getCharacter().charAt(0));
		} catch (IOException e) {
			Log.e("nakama", "Error: can't find kanji for: " + this.kanji, e);
			Toast.makeText(activity, "Internal Error: can't find kanji information for: " + this.kanji, Toast.LENGTH_LONG).show();
		} finally {
			sd.close();
		}
		this.currentCharacterSvg = parent.getCurrentCharacterSvg();
		this.dictSet = sd;
		
		super.onAttach(activity);
	}

    @Override public void onDetach(){
        this.searchTask.cancel(true);
        super.onDetach();
    }
	
	 @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d("nakama", "TeachingInfoFragment onCreateView: thi.kanjis is " + this.kanji);
		view = inflater.inflate(R.layout.activity_teaching, container, false);

		Glyph animGlyph = new Glyph(currentCharacterSvg);
		this.kanim = (AnimatedCurveView)view.findViewById(R.id.kanji_animation);
		this.kanim.setDrawing(animGlyph, AnimatedCurveView.DrawTime.ANIMATED);
		this.kanim.startAnimation(500);
		
		kanjiLabel = (TextView)view.findViewById(R.id.bigkanji);
		kanjiLabel.setText(Character.toString(this.kanji.kanji));
		
		this.usedInTranslations = new LinkedList<>();

		addTextViewsToLayout((LinearLayout)view.findViewById(R.id.meanings), this.kanji.meanings);
	
		this.examplesLayout = (LinearLayout)view.findViewById(R.id.exampleSpace);

         // TODO: this should probably be made into a RecyclerView
		AddTranslation adder = new AddTranslation(){
			public void add(Translation t){
				Log.i("nakama", "Inflating translation View for: " + t.toString());
				View newTranslation = View.inflate(activity, R.layout.translation_slide, null);
				AdvancedFuriganaTextView af = (AdvancedFuriganaTextView) newTranslation.findViewById(R.id.kanji);
				af.setTranslation(t, dictSet.kanjiFinder());
				TextView eng = (TextView) newTranslation.findViewById(R.id.english);
				eng.setText(t.toEnglishString());
				examplesLayout.addView(newTranslation);
			}
		};
        
		this.searchTask = new KanjiTranslationListAsyncTask(adder, this.dictSet, this.kanji.kanji);
		this.searchTask.execute();			
		
        return view;	
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
