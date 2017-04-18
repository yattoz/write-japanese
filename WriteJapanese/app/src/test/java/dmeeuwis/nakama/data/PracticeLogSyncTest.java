package dmeeuwis.nakama.data;

import android.app.Activity;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import org.apache.tools.ant.filters.StringInputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import dmeeuwis.kanjimaster.BuildConfig;
import dmeeuwis.nakama.primary.KanjiMasterActivity;

import static junit.framework.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class PracticeLogSyncTest {

    private Context ctx;

    private static class TestDependencies extends PracticeLogSync.ExternalDependencies {
        private final String file;
        public TestDependencies(Context ctx, String jsonFile){
            super(ctx);
            this.file = jsonFile;
        }
        @Override
        public InputStream sendPost(String jsonPost) throws Exception {
            return new StringInputStream(readTestData(file));
        }
    }


    @Before
    public void setUp() throws Exception {
        Activity activity = Robolectric.buildActivity(KanjiMasterActivity.class).create().resume().get();
        ctx = activity.getApplicationContext();
        ctx.deleteDatabase(WriteJapaneseOpenHelper.DB_NAME);
    }

    @Test
    public void testEmptySync() throws IOException {
        PracticeLogSync sync = new PracticeLogSync(new TestDependencies(ctx, "empty_sync.json"), ctx);
        sync.sync();
    }

    @Test
    public void testEmptySyncBeforeCharsetEdits() throws IOException {
        PracticeLogSync sync = new PracticeLogSync(new TestDependencies(ctx, "empty_sync_no_charset_edits.json"), ctx);
        sync.sync();
    }

    // test practice logs sync

    // test charset goals edit sync

    // test charset edits sync


    public static String readTestData(String filename) throws Exception {
        java.net.URL url = PracticeLogSyncTest.class.getResource(filename);
        if(url == null){ throw new RuntimeException("Cannot get test resource data"); }
        File f = new File(url.toURI());
        if(!f.exists()){ throw new RuntimeException("Can't find test data: " + f); };
        String json = new java.util.Scanner(f, "UTF-8").useDelimiter("\\Z").next();
        System.out.println("Read test input json as: " + json);
        return json;
    }
}
