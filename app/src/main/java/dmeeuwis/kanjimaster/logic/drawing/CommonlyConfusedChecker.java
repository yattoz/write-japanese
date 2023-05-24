package dmeeuwis.kanjimaster.logic.drawing;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import dmeeuwis.kanjimaster.logic.data.Point;

class CommonlyConfusedChecker {

    private static class SimilarEndingConfusion {
        static List<SimilarEndingConfusion> END_LOOPERS = Arrays.asList(
                // note: mo's stroke order is funny, so check both first and last stroke for self-intersect?
                new SimilarEndingConfusion('ま', 'も',"ma", "mo", 2, 1),
                new SimilarEndingConfusion('ね', 'れ',"ne", "re", 1, 1),
//          new SimilarEndingConfusion('る', 'ろ',"ru", "ro", 0, 1),
                new SimilarEndingConfusion('ぬ', 'め',"nu", "me", 1, 2)
        );

        char a, b;
        String readingA, readingB;
        int selfIntersect;
        int strokeIndex;

        public SimilarEndingConfusion(char a, char b, String readingA, String readingB, int strokeIndex, int selfIntersects) {
            this.a = a;
            this.b = b;
            this.readingA = readingA;
            this.readingB = readingB;
            this.strokeIndex = strokeIndex;
            this.selfIntersect = selfIntersects;
        }
    }

    public static void checkEasilyConfusedCharacters(char target, PointDrawing drawn, Criticism c){
        for(SimilarEndingConfusion confusion: SimilarEndingConfusion.END_LOOPERS) {
            boolean hasEnoughStrokes = drawn.strokeCount() > confusion.strokeIndex;

            if (target == confusion.a && hasEnoughStrokes) {
                Set<Point> selfHits = PathCalculator.intersections(drawn.get(confusion.strokeIndex), drawn.get(confusion.strokeIndex));
                if (selfHits.size() <= confusion.selfIntersect - 1) {
                    c.add(String.format("The last stroke of %s should loop over itself; otherwise it looks like %s '%s'",
                            confusion.a, confusion.b, confusion.readingB),
                            Criticism.SKIP, Criticism.SKIP);
                }
            }

            if (target == confusion.b && hasEnoughStrokes) {
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
