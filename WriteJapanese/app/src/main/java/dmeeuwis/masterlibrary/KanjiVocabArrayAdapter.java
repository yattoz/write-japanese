package dmeeuwis.masterlibrary;

import java.util.Set;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import dmeeuwis.Translation;
import dmeeuwis.indexer.KanjiFinder;
import dmeeuwis.kanjimaster.R;
import dmeeuwis.nakama.views.AdvancedFuriganaTextView;

public class KanjiVocabArrayAdapter extends com.nhaarman.listviewanimations.ArrayAdapter<Translation> {

	public final static int MAX_ENGLISH_LENGTH_PORTRAIT = 75;
	public final static int MAX_ENGLISH_LENGTH_LANDSCAPE = 165;
	
	final Activity context;
	final KanjiFinder kanjiFinder;
	
	boolean isPortraitOrientation = true;
	public Integer currentSelectedId;
	public Set<Integer> favoritesList;

	@Override public boolean hasStableIds(){
		return true;
	}

	public KanjiVocabArrayAdapter(Activity context, KanjiFinder kanjiFinder) {
		super();
		this.context = context;
		this.kanjiFinder = kanjiFinder;
	}

	@Override
	public View getView(final int position, final View convertView, final ViewGroup parent) {
		View view = convertView;
		if(view == null){
			LayoutInflater inflater = this.context.getLayoutInflater();
			view = inflater.inflate(R.layout.translation_slide, parent, false);
		} 

		final Translation t = this.getItem(position);
		
		AdvancedFuriganaTextView kanji = (AdvancedFuriganaTextView) view.findViewById(R.id.kanji);
		kanji.setTranslation(t, this.kanjiFinder);
		
		TextView english = (TextView) view.findViewById(R.id.english);
		
		String englishText = t.toEnglishString();
		final int engMaxLength = this.isPortraitOrientation ? MAX_ENGLISH_LENGTH_PORTRAIT : MAX_ENGLISH_LENGTH_LANDSCAPE;
		if(englishText.length() > engMaxLength)
			englishText = englishText.substring(0, engMaxLength) + "...";
		english.setText(englishText);

		return view;
	}
}