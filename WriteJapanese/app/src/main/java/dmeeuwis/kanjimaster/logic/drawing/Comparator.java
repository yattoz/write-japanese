package dmeeuwis.kanjimaster.logic.drawing;

import java.io.IOException;

public interface Comparator {
    Criticism compare(char target, PointDrawing drawn, CurveDrawing known) throws IOException;
}
