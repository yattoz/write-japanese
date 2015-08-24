package dmeeuwis.nakama.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import dmeeuwis.kanjimaster.R;

public class CircleLabel extends View {
    private int default_color = Color.LTGRAY;
    private String label;
    private int colour = Color.RED;

    private TextPaint mTextPaint;
    private Paint circlePaint;
    private float mTextWidth;
    private float mTextHeight;

    public CircleLabel(Context context) {
        super(context);
        init(null, 0);
    }

    public CircleLabel(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public CircleLabel(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.CircleLabel, defStyle, 0);

        label = a.getString( R.styleable.CircleLabel_exampleString);
        colour = a.getColor(R.styleable.CircleLabel_exampleColor, default_color);

        a.recycle();

        // Set up a default TextPaint object
        mTextPaint = new TextPaint();
        mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.LEFT);

        circlePaint = new Paint();
        circlePaint.setAntiAlias(true);
        circlePaint.setColor(colour);
    }

    public void setLabel(String newLabel){
        this.label = newLabel;
        this.invalidate();
    }

    public void setColor(int colour){
        this.circlePaint.setColor(colour);
        this.invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // TODO: consider storing these as member variables to reduce
        // allocations per draw cycle.
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        int contentWidth = getWidth() - paddingLeft - paddingRight;
        int contentHeight = getHeight() - paddingTop - paddingBottom;

        canvas.drawCircle(0, 0, contentWidth / 2, circlePaint);

        // Draw the text.
        canvas.drawText(label,
                paddingLeft + (contentWidth - mTextWidth) / 2,
                paddingTop + (contentHeight + mTextHeight) / 2,
                mTextPaint);
    }

}
