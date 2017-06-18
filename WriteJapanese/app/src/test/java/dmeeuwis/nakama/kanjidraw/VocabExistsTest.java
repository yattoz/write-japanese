package dmeeuwis.nakama.kanjidraw;

import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dmeeuwis.Kana;
import dmeeuwis.Kanji;
import dmeeuwis.Translation;
import dmeeuwis.indexer.KanjiFinder;
import dmeeuwis.indexer.Querier;
import dmeeuwis.indexer.QuerierRandomAccessFile;
import dmeeuwis.nakama.data.CharacterSets;
import dmeeuwis.nakama.data.CharacterStudySet;
import dmeeuwis.nakama.data.TranslationsFromXml;

import static org.junit.Assert.assertTrue;

public class VocabExistsTest {
    public static final String KANJIDICT_FILE = "kanjidic.utf8.awb";
    public static final String KANJIDICT_INDEX = "kanjidic.index.awb";


    @Test
    public void testVocabExistsForAllKanji() throws Exception {
        String path = System.getProperty("user.dir") + "/app/src/main/assets/char_vocab/";

        List<String> failures = new ArrayList<>();
        String[] sets = new String[]{
                Kana.commonHiragana(),
                Kana.commonKatakana(),
                Kanji.JOUYOU_G1,
                Kanji.JOUYOU_G2,
                Kanji.JOUYOU_G3,
                Kanji.JOUYOU_G4,
                Kanji.JOUYOU_G5,
                Kanji.JOUYOU_G6,
        };

        for(String s: sets){
            for(Character c: s.toCharArray()){

                String filename = Integer.toHexString((c).charValue()) + "_trans.xml";
                FileInputStream fin = new FileInputStream(path + filename);

                TranslationsFromXml t = new TranslationsFromXml();

                final List<Translation> collect = new ArrayList<>();
                TranslationsFromXml.PublishTranslation p = new TranslationsFromXml.PublishTranslation() {
                    @Override
                    public void publish(Translation t) {
                       collect.add(t);
                    }
                };

                t.load(fin, p);

                boolean success = collect.size() > 0;
                if(!success){
                    failures.add(c + " has no translations.");
                }
            }
        }

        for(String s: failures){ System.out.println(s); }
        assertTrue("All characters have translations", failures.size() == 0);
    }
}
