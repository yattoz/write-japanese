package dmeeuwis.nakama;


import java.util.concurrent.atomic.AtomicInteger;

public class AndroidUtil {
    private static final AtomicInteger nextId = new AtomicInteger(1);

    public static int generateViewId() {
        while(true){
            final int result = nextId.get();
            // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
            int newValue = result + 1;
            if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
            if (nextId.compareAndSet(result, newValue)) {
                return result;
            }
        }
    }
}
