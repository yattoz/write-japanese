package dmeeuwis.nakama.kanjidraw;

import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import dmeeuwis.Translation;
import dmeeuwis.indexer.KanjiFinder;
import dmeeuwis.indexer.Querier;
import dmeeuwis.indexer.QuerierRandomAccessFile;
import dmeeuwis.nakama.data.CharacterSets;
import dmeeuwis.nakama.data.CharacterStudySet;

import static org.junit.Assert.assertTrue;

public class VocabExistsTest {
    public static final String KANJIDICT_FILE = "kanjidic.utf8.awb";
    public static final String KANJIDICT_INDEX = "kanjidic.index.awb";

    public static final String SMALL_EDICT_FILE = "dict.ichi1.subbed.indexed.awb";
    public static final String SMALL_SEARCH_HASH_TO_EDICT_ID = "index.ichi1.searchHashToEdictId.awb";
    public static final String SMALL_EDICT_ID_TO_LOCATION_SIZE = "index.ichi1.edictIdToLocationSize.awb";

    @Test
    public void testVocabExistsForAllKanji() throws Exception {
        String path = System.getProperty("user.dir") + "/src/main/assets/";

        KanjiFinder kf;
        {
            File indexFile = new File(path + KANJIDICT_INDEX);
            InputStream index = new FileInputStream(indexFile);
            RandomAccessFile dict = new RandomAccessFile(path + KANJIDICT_FILE, "r");
            kf = new KanjiFinder(index, indexFile.length(), dict.getChannel(), 0);
        }

        RandomAccessFile dict = new RandomAccessFile(path + SMALL_EDICT_FILE, "r");
        RandomAccessFile hashToEdictId = new RandomAccessFile(path + SMALL_SEARCH_HASH_TO_EDICT_ID, "r");
        RandomAccessFile locsize = new RandomAccessFile(path + SMALL_EDICT_ID_TO_LOCATION_SIZE, "r");

        List<String> failures = new ArrayList<>();
        CharacterStudySet[] sets = CharacterSets.all(kf, null, null);
        for(CharacterStudySet s: sets){
            for(Character c: s.allCharactersSet){
                Querier q = new QuerierRandomAccessFile(hashToEdictId, locsize, dict);
                List<Translation> t = q.singleCharacterSearch(1, 0, c);
                boolean success = t != null && t.size() > 0;
                if(!success){
                    failures.add(s.pathPrefix + " " + c + " has no translations.");
                }
            }
        }

        for(String s: failures){ System.out.println(s); }
        assertTrue("All characters have translations", failures.size() == 0);
    }
}
