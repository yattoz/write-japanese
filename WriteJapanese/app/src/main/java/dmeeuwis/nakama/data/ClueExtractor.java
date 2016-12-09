package dmeeuwis.nakama.data;

import java.io.IOException;

import dmeeuwis.Kana;
import dmeeuwis.Kanji;
import dmeeuwis.Translation;
import dmeeuwis.indexer.KanjiFinder;
import dmeeuwis.util.Util;

public class ClueExtractor {

    private final KanjiFinder kanjiFinder;

    public ClueExtractor(KanjiFinder kf) {
        this.kanjiFinder = kf;
    }

    public String[] readingClues(Character currentCharacter) {
        // clue from readings
        if (Kana.isKanji(currentCharacter)) {
            try {
                Kanji k = kanjiFinder.find(currentCharacter);
                String[] readings = Util.concat(k.onyomi, k.kunyomi);
                return readings;
            } catch (IOException e) {
                // fall through
            }
        }
        return null;
    }

    public String[] meaningsClues(Character currentCharacter) {
        try {
            Kanji k = kanjiFinder.find(currentCharacter);
            return k.meanings;
        } catch (IOException e) {
            return null;
        }
    }


    public Translation TranslationsClue(Character currentCharacter, int index) throws IOException {
        return null;
    }
}
