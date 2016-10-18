package dmeeuwis.nakama.kanjidraw;

public interface Comparator {
    Criticism compare(char target, PointDrawing drawn, CurveDrawing known);
}
