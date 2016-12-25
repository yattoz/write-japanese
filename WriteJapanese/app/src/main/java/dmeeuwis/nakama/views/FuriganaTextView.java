package dmeeuwis.nakama.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import dmeeuwis.KanjiElement;
import dmeeuwis.KanjiElement.Furigana;
import dmeeuwis.Translation;
import dmeeuwis.indexer.KanjiFinder;
import dmeeuwis.kanjimaster.R;

public class FuriganaTextView extends View {

	Furigana[] parts;
	int kanjiWidths[];
	int furiWidths[];
	int maxKanjiHeight, maxFuriHeight;
	int textWidth;
	float characterPadding;

	private Paint mainPaint;
	private Paint furiganaPaint;

	private float padding = 10;
	
	public FuriganaTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(attrs);
		
	}

	public FuriganaTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs);
	}

	public FuriganaTextView(Context context) {
		super(context);
		init(null);
	}

	private void init(AttributeSet attrs){
    	this.mainPaint = new Paint();
		this.mainPaint.setColor(Color.BLACK);
        this.mainPaint.setTextSize(56);
		this.mainPaint.setAntiAlias(true);

    	this.furiganaPaint = new Paint();
		this.furiganaPaint.setColor(0xFF878787);
		this.furiganaPaint.setTextSize(16);
		this.furiganaPaint.setAntiAlias(true);

        if(attrs != null){
            TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.FuriganaTextView);
            int mainTextSize = typedArray.getDimensionPixelSize(R.styleable.FuriganaTextView_mainTextSize, 56);
            int furiTextSize = typedArray.getDimensionPixelSize(R.styleable.FuriganaTextView_furiganaTextSize, 16);
            typedArray.recycle();

            this.mainPaint.setTextSize(mainTextSize);
            this.furiganaPaint.setTextSize(furiTextSize);
        }
	}

	public void setTranslation(Translation t, KanjiFinder finder){
		if(t == null) throw new NullPointerException("Null translation passed in.");
		if(finder == null) throw new NullPointerException("Null KanjiFinder passed in.");
		
        KanjiElement kanji = t.getFirstKanjiElement();
		if(kanji != null){
			this.parts = kanji.getFuriganaBreakdown(t.toReadingString(), finder);
		} else {
			// kana-only word
			this.parts = new Furigana[] { new Furigana(t.toKanjiString(), "")};
		}
        this.kanjiWidths = new int[this.parts.length];
        this.furiWidths = new int[this.parts.length];

        calculateTextBounds();
        this.requestLayout();
        this.invalidate();
	}


    public void setTranslationQuiz(Translation t, Character targetChar, KanjiFinder finder){
        if(t == null) throw new NullPointerException("Null translation passed in.");
        if(finder == null) throw new NullPointerException("Null KanjiFinder passed in.");

		String targetCharString = String.valueOf(targetChar);

        KanjiElement kanji = t.getMatchingKanjiElement(targetChar);
        if(kanji != null){
			this.parts = kanji.getFuriganaBreakdown(t.toReadingString(), finder);

            for(int i = 0; i < this.parts.length; i++){
                if (this.parts[i].kanji.contains(targetCharString)){
                    this.parts[i] = new Furigana(this.parts[i].kanji.replaceAll(targetCharString, "?"), this.parts[i].furigana);
				}
            }
        } else {
            // kana-only word
            this.parts = new Furigana[] { new Furigana(t.toKanjiString(), "")};
        }
        this.kanjiWidths = new int[this.parts.length];
        this.furiWidths = new int[this.parts.length];

        calculateTextBounds();
        this.requestLayout();
        this.invalidate();
    }

	private void calculateTextBounds(){
        Rect bounds = new Rect();
        textWidth = 0;
		if(this.parts == null) { return; }

        for(int i = 0; i < this.parts.length; i++){
        	Furigana f = this.parts[i];
        	this.mainPaint.getTextBounds(f.kanji, 0, f.kanji.length(), bounds);
			this.maxKanjiHeight = Math.max(maxKanjiHeight, bounds.height());
        	kanjiWidths[i] = bounds.width();

			if(f.furigana != null){
				this.furiganaPaint.getTextBounds(f.furigana, 0, f.furigana.length(), bounds);
				this.maxFuriHeight = Math.max(maxFuriHeight, bounds.height());
				furiWidths[i] = bounds.width();
			} else {
				furiWidths[i] = kanjiWidths[i];
			}
			maxFuriHeight = Math.max(maxFuriHeight, bounds.height());
			furiWidths[i] = bounds.width();
			
			textWidth += Math.max(furiWidths[i], kanjiWidths[i]);
        }
        
        characterPadding = (float)((textWidth / this.parts.length) * 0.05);
		textWidth += characterPadding * this.parts.length + 2 * characterPadding;
		
        this.invalidate();
	}

    public void setTextAndReadingSizesDp(int mainSizeDp, int furiganaSizeDp){
        int mainSizePx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, mainSizeDp, this.getResources().getDisplayMetrics());
        int furiganaSizePx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, furiganaSizeDp, this.getResources().getDisplayMetrics());
        this.setTextAndReadingSizes(mainSizePx, furiganaSizePx);
    }

	public void setTextAndReadingSizes(int mainSize, int furiganaSize){
		this.mainPaint.setTextSize(mainSize);
		this.furiganaPaint.setTextSize(furiganaSize);
		calculateTextBounds();
		this.invalidate();
	}

	public void setPadding(int padding){
		this.padding = padding;
		this.invalidate();
	}

	@Override protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec){
		if(this.parts == null){
			setMeasuredDimension(0, 0);
			return;
		}
	
        float desiredWidth = textWidth + 2*this.padding + (this.parts.length - 1) * characterPadding;
        desiredWidth += this.padding; // hack until I figure out why some edges are slightly cut off.
        float desiredHeight = maxKanjiHeight + maxFuriHeight + 2*this.padding + (maxKanjiHeight/4);
        
        int measuredWidth = (int)(MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY ? 
        		View.MeasureSpec.getSize(widthMeasureSpec) : desiredWidth);
        int measuredHeight = (int)(MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY ? 
        		View.MeasureSpec.getSize(heightMeasureSpec) : desiredHeight);
        
		setMeasuredDimension(measuredWidth, measuredHeight);
	}

	@Override protected void onDraw(Canvas canvas){
		if(this.parts == null){
			return;
		}
		
		int furiGap = maxKanjiHeight / 4;

		//Log.i("nakama", "onDraw!\n");
		float currentX = this.padding;
		for(int i = 0; i < this.parts.length; i++){
			Furigana s = this.parts[i];
			float partWidth = Math.max(this.kanjiWidths[i], this.furiWidths[i]) + characterPadding*2; // + inter-char padding
			//Log.i("nakama", "Drawing: furigana " + this.parts[i] + " has part with " + partWidth + "; currentX is " + currentX);

			float fWidth = kanjiWidths[i];
			float inPartPadding = (partWidth - fWidth) / 2;
			
			canvas.drawText(s.kanji, currentX + inPartPadding, maxKanjiHeight + this.padding, this.mainPaint);
			//Log.i("nakama", "Drew " + s.kanji + " at " + (currentX + inPartPadding) + ", " + (maxKanjiHeight + this.padding) + "; width is " + fWidth);

			if(s.furigana != null){
				fWidth = furiWidths[i];
				inPartPadding = (partWidth - fWidth) / 2;
				float furiXStart = currentX + inPartPadding;
				float furiYStart = maxKanjiHeight + this.padding + maxFuriHeight + furiGap;
				canvas.drawText(s.furigana, furiXStart, furiYStart, this.furiganaPaint);
				//Log.i("nakama", "Draw measure " + s.furigana + " as fWidth " + fWidth + " vs furiWidths value " + this.furiWidths[i] + "; and padding " + inPartPadding + ". partWidth is " + partWidth);
				//Log.i("nakama", "Drew secondary " + s.furigana + " at " + furiXStart + ", " + furiYStart);
			}
			currentX += partWidth;
		}
		//Log.i("nakama", "At end of onDraw, currentX is " + currentX + "; width was measured at " + getMeasuredWidth());
	}
}
