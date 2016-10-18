package dmeeuwis.nakama.kanjidraw;

import android.content.Context;

import dmeeuwis.nakama.data.AssetFinder;
import dmeeuwis.nakama.data.Settings;

public class ComparisonFactory {
    public static Comparator getUsersComparator(Context ctx, AssetFinder assetFinder){
        if(Settings.getStrictness(ctx) == Settings.Strictness.CASUAL) {
            return new SimpleDrawingComparator(assetFinder, SimpleDrawingComparator.StrokeOrder.DISCOUNT);
        } else if(Settings.getStrictness(ctx) == Settings.Strictness.CASUAL_ORDERED){
                return new SimpleDrawingComparator(assetFinder, SimpleDrawingComparator.StrokeOrder.COUNT);
        } else {
            return new DrawingComparator(assetFinder);
        }
    }

}
