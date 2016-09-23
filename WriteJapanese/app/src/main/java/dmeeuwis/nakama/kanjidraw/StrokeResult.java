package dmeeuwis.nakama.kanjidraw;

import java.util.Locale;

/**
 * Created by dmeeuwis on 22/09/16.
 */
class StrokeResult {
    public final Integer knownStrokeIndex;
    public final Integer drawnStrokeIndex;
    public final int score;

	public StrokeResult(Integer known, Integer drawn, int score) {
		this.knownStrokeIndex = known;
        this.drawnStrokeIndex = drawn;
        this.score = score;
    }

	public String toString() {
		return String.format(Locale.ENGLISH, "Known %d matched drawn %d with score %d",
				knownStrokeIndex, drawnStrokeIndex, score);
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StrokeResult)) return false;

        StrokeResult that = (StrokeResult) o;

        if (score != that.score) return false;
        if (knownStrokeIndex != null ? !knownStrokeIndex.equals(that.knownStrokeIndex) : that.knownStrokeIndex != null)
            return false;
        return drawnStrokeIndex != null ? drawnStrokeIndex.equals(that.drawnStrokeIndex) : that.drawnStrokeIndex == null;

    }

    @Override
    public int hashCode() {
        int result = knownStrokeIndex != null ? knownStrokeIndex.hashCode() : 0;
        result = 31 * result + (drawnStrokeIndex != null ? drawnStrokeIndex.hashCode() : 0);
        result = 31 * result + score;
        return result;
    }
}
