package dmeeuwis.kanjimaster.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.GridView;

/**
 * A gridview that will show standardSets of its rows (up to some arbitrary max height).
 */
public class TallGridView extends GridView {
    public TallGridView(Context context) {
        super(context);
    }
 
    public TallGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
 
    public TallGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

	protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
        int heightSpec;
        if (getLayoutParams().height == LayoutParams.WRAP_CONTENT) {
            // The great Android "hackatlon", the love, the magic.
            // The two leftmost bits in the height measure spec have
            // a special meaning, hence we can't use them to describe height.
            heightSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE << 2, MeasureSpec.AT_MOST);
        }
        else {
            // Any other height should be respected as is.
            heightSpec = heightMeasureSpec;
        }
 
        super.onMeasure(widthMeasureSpec, heightSpec);
	}
}