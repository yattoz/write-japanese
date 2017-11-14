package dmeeuwis.nakama.data;

import org.junit.Test;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import dmeeuwis.Kana;
import dmeeuwis.Kanji;
import dmeeuwis.Translation;

import static org.junit.Assert.assertTrue;

public class VocabExistsTest {
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
                Kanji.JOUYOU_SS,
                Kanji.JLPT_N5,
                Kanji.JLPT_N4,
                Kanji.JLPT_N3,
                Kanji.JLPT_N2,
                Kanji.JLPT_N1,
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
