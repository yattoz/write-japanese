package dmeeuwis.nakama.teaching;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class MyViewPager extends ViewPager {

    private boolean motionEnabled = true;

    public MyViewPager(Context context) {
        super(context);
    }

    public MyViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    public void setMotionEnabled(boolean enabled){
        this.motionEnabled = enabled;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e){
        return motionEnabled;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e){
        return !motionEnabled;
    }
}
