package dmeeuwis.kanjimaster.logic.data;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.List;

import dmeeuwis.kanjimaster.core.Translation;
import dmeeuwis.kanjimaster.core.indexer.KanjiFinder;

public interface DictionarySet {
    KanjiFinder kanjiFinder();

    List<Translation> loadTranslations(Character kanji, int limit) throws IOException, XmlPullParserException;

    void close();
}
