package dmeeuwis.nakama.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class DrawTest extends View implements View.OnTouchListener {

    List<PointF> line = new ArrayList<>(200);
    Paint paint = new Paint();

    public DrawTest(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        this.paint = new Paint();
        this.paint.setStyle(Paint.Style.STROKE);
        this.paint.setStrokeCap(Paint.Cap.ROUND);
        this.paint.setAntiAlias(true);
        this.paint.setDither(true);
        this.paint.setStrokeWidth(5);
        this.paint.setColor(Color.BLACK);
    }

    public DrawTest(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DrawTest(Context context) {
        this(context, null, 0);
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.d("nakama", "DrawTest.onTouch");
        for(int i = 0; i < event.getHistorySize(); i++){
           line.add(new PointF(event.getHistoricalX(i), event.getHistoricalY(i)));
        }
        line.add(new PointF(event.getX(), event.getY()));
        this.invalidate();
        return true;
    }

    @Override
    public void onDraw(Canvas canvas){
        Log.d("nakama", "DrawTest.onDraw: " + this.getWidth() + ", " + this.getHeight());
        for(int i = 1; i < line.size()+1; i++){
            canvas.drawLine(line.get(i-1).x, line.get(i-1).y, line.get(i).x, line.get(i).y, paint);
        }
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
    }
}
