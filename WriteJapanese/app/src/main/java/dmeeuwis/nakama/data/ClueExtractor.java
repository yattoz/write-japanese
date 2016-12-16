package dmeeuwis.nakama.data;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.List;

import dmeeuwis.Kana;
import dmeeuwis.Kanji;
import dmeeuwis.Translation;
import dmeeuwis.indexer.KanjiFinder;
import dmeeuwis.util.Util;

public class ClueExtractor {

    private final DictionarySet set;

    public ClueExtractor(DictionarySet set) {
        this.set = set;
    }

    public String[] readingClues(Character currentCharacter) {
        // clue from readings
        if (Kana.isKanji(currentCharacter)) {
            try {
                Kanji k = set.kanjiFinder().find(currentCharacter);
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
            Kanji k = set.kanjiFinder().find(currentCharacter);
            return k.meanings;
        } catch (IOException e) {
            return null;
        }
    }


    public Translation translationsClue(Character currentCharacter, int index) {
        try {
            List<Translation> t = set.querier.orQueries(0, 1, new String[] { String.valueOf(currentCharacter) });
            return t.get(0);
        } catch (IOException|XmlPullParserException e) {
            return null;
        }
    }

    public DictionarySet getDictionarySet(){
        return set;
    }
}
