package dmeeuwis.nakama.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class PieProgressView extends View {

    Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    RectF mInnerBoundsF = new RectF(0, 0, 0, 0);

    int known, studying, reviewing, unknown;

    public PieProgressView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public PieProgressView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PieProgressView(Context context) {
        this(context, null, 0);
    }

    public void setProgressLevels(int known, int studying, int reviewing, int unknown){
        this.known = known;
        this.studying = studying;
        this.reviewing = reviewing;
        this.unknown = unknown;
        this.invalidate();
    }

    @Override protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec){
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        MeasureUtil.WidthAndHeight wh = MeasureUtil.fillMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(wh.width, wh.height);
        mInnerBoundsF.set(0, 0, wh.width, wh.height);
    }

    @Override
    public void draw(Canvas canvas) {
        // Rotate the canvas around the center of the pie by 90 degrees
        // counter clockwise so the pie stars at 12 o'clock.
        canvas.rotate(-90f, getWidth()/2, getHeight()/2);
        mPaint.setStyle(Paint.Style.STROKE);
        canvas.drawOval(mInnerBoundsF, mPaint);
        mPaint.setStyle(Paint.Style.FILL);

        final float drawTo = ((float)360*known)/100f;
        float drawTo2 = ((float)360*(known + studying))/100f;
        float drawTo3 = ((float)360*(known + studying + reviewing))/100f;
        float drawTo4 = ((float)360*(known + studying + reviewing + unknown))/100f;

        mPaint.setColor(Color.GRAY);
        canvas.drawArc(mInnerBoundsF, 0, drawTo4, true, mPaint);
        mPaint.setColor(Color.RED);
        canvas.drawArc(mInnerBoundsF, 0, drawTo3, true, mPaint);
        mPaint.setColor(Color.BLUE);
        canvas.drawArc(mInnerBoundsF, 0, drawTo2, true, mPaint);
        mPaint.setColor(Color.GREEN);
        canvas.drawArc(mInnerBoundsF, 0, drawTo, true, mPaint);

        mPaint.setColor(Color.WHITE);
        float radius = mInnerBoundsF.width() / 4;
        canvas.drawCircle(mInnerBoundsF.centerX(), mInnerBoundsF.centerY(), radius, mPaint);

        // Draw inner oval and text on top of the pie (or add any other
        // decorations such as a stroke) here..
        // Don't forget to rotate the canvas back if you plan to add text!
    }
}
