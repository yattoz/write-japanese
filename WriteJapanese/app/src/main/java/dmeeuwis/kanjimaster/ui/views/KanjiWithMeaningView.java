package dmeeuwis.kanjimaster.ui.views;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class KanjiWithMeaningView extends LinearLayout {
	
	private TextView kanjiText;
	private TextView meaningText;

	public KanjiWithMeaningView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	public KanjiWithMeaningView(Context context) {
		super(context);
		init();
	}
	
	private void init(){
		this.setLayoutParams(new AbsListView.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		this.setOrientation(VERTICAL);
		
		Resources r = getContext().getResources();
		int padding_px = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, r.getDisplayMetrics());	
		this.setPadding(0, 0, 0, padding_px);
		
		kanjiText = new TextView(this.getContext());
		kanjiText.setGravity(Gravity.CENTER);
		kanjiText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 48);
		
		meaningText = new TextView(this.getContext());
		meaningText.setGravity(Gravity.CENTER);
		kanjiText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
		
		LinearLayout.LayoutParams childParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		
		this.addView(kanjiText, childParams);
		this.addView(meaningText, childParams);
	}
	
	public void setKanjiAndMeaning(String kanji, String meaning){
		kanjiText.setText(kanji);
		meaningText.setText(meaning);
	}
}
