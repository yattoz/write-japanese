package dmeeuwis.kanjimaster.logic.drawing;

import android.content.Context;

import dmeeuwis.kanjimaster.logic.data.AssetFinder;
import dmeeuwis.kanjimaster.logic.data.Settings;
import dmeeuwis.kanjimaster.logic.data.UncaughtExceptionLogger;

public class ComparisonFactory {
    public static Comparator getUsersComparator(Context ctx, AssetFinder assetFinder){
        try {
            if (Settings.getStrictness(ctx) == Settings.Strictness.CASUAL) {
                return new SimpleDrawingComparator(assetFinder, SimpleDrawingComparator.StrokeOrder.DISCOUNT);
            } else if (Settings.getStrictness(ctx) == Settings.Strictness.CASUAL_ORDERED) {
                return new SimpleDrawingComparator(assetFinder, SimpleDrawingComparator.StrokeOrder.COUNT);
            } else if (Settings.getStrictness(ctx) == Settings.Strictness.STRICT) {
                return new DrawingComparator(assetFinder);
            } else {
                return new SimpleDrawingComparator(assetFinder, SimpleDrawingComparator.StrokeOrder.DISCOUNT);
            }
        } catch(Throwable t){
            UncaughtExceptionLogger.backgroundLogError("HACK: exception while getting ComparisonFactory! Returning default instead.", t, ctx);
            return new SimpleDrawingComparator(assetFinder, SimpleDrawingComparator.StrokeOrder.DISCOUNT);
        }
    }
}