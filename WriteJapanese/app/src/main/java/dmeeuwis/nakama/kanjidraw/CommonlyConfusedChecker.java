package dmeeuwis.nakama.kanjidraw;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import dmeeuwis.nakama.data.Point;

public class CommonlyConfusedChecker {

    private static class Confusion {
        char a, b;
        String readingA, readingB;
        int selfIntersect;
        int strokeIndex;

        public Confusion(char a, char b, String readingA, String readingB, int strokeIndex, int selfIntersects) {
            this.a = a;
            this.b = b;
            this.readingA = readingA;
            this.readingB = readingB;
            this.strokeIndex = strokeIndex;
            this.selfIntersect = selfIntersects;
        }
    }

    private static List<Confusion> END_LOOPERS = Arrays.asList(
            // note: mo's stroke order is funny, so check both first and last stroke for self-intersect?
            new Confusion('ま', 'も',"ma", "mo", 2, 1),
            new Confusion('ね', 'れ',"ne", "re", 1, 1),
//          new Confusion('る', 'ろ',"ru", "ro", 0, 1),
            new Confusion('ぬ', 'め',"nu", "me", 1, 2)
    );

    public static void checkEasilyConfusedCharacters(char target, PointDrawing drawn, Criticism c){
        for(Confusion confusion: END_LOOPERS) {
            if (target == confusion.a) {
                Set<Point> selfHits = PathCalculator.intersections(drawn.get(confusion.strokeIndex), drawn.get(confusion.strokeIndex));
                if (selfHits.size() <= confusion.selfIntersect - 1) {
                    c.add(String.format("The last stroke of %s should loop over itself; otherwise it looks like %s '%s'",
                            confusion.a, confusion.b, confusion.readingB),
                            Criticism.SKIP, Criticism.SKIP);
                }
            }

            if (target == confusion.b) {
                Set<Point> selfHits = PathCalculator.intersections(drawn.get(confusion.strokeIndex), drawn.get(confusion.strokeIndex));
                if (selfHits.size() > confusion.selfIntersect - 1) {
                    c.add(String.format("The last stroke of %s should not loop over itself; otherwise it becomes %s '%s'",
                            confusion.b, confusion.a, confusion.readingA),
                            Criticism.SKIP, Criticism.SKIP);
                }
            }
        }
    }
}
