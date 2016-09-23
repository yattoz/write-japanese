package dmeeuwis.nakama.kanjidraw;

import android.content.Context;

import dmeeuwis.nakama.data.AssetFinder;
import dmeeuwis.nakama.data.Settings;

public class ComparisonFactory {
    public static Comparator getUsersComparator(Context ctx, char target, CurveDrawing known, PointDrawing challenger, AssetFinder assetFinder){
        if(Settings.getStrictness(ctx) == Settings.Strictness.CASUAL){
            return new SimpleDrawingComparator(target, known, challenger, assetFinder);
        } else {
            return new DrawingComparator(target, known, challenger, assetFinder);
        }
    }

}
