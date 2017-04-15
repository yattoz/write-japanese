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

    private WriteJapaneseOpenHelper helper;
    private Context ctx;

    @Before
    public void setUp() throws Exception {
        Activity activity = Robolectric.buildActivity(KanjiMasterActivity.class).create().resume().get();
        ctx = activity.getApplicationContext();
        ctx.deleteDatabase(WriteJapaneseOpenHelper.DB_NAME);
        helper = new WriteJapaneseOpenHelper(ctx);
    }

    @After
    public void tearDown() throws Exception {
//        helper.close();
    }

    @Test
    public void testSyncIn() throws IOException {

        PracticeLogSync.ExternalDependencies ep = new PracticeLogSync.ExternalDependencies(ctx){
            @Override
            public InputStream sendPost(String jsonPost) throws Exception {
                return new StringInputStream(readXMLToString());
            }
        };

        PracticeLogSync sync = new PracticeLogSync(ep, ctx);
        sync.sync();

    }

    public String readXMLToString() throws Exception {
        java.net.URL url = PracticeLogSyncTest.class.getResource("charset_sync_data.json");
        if(url == null){ throw new RuntimeException("Cannot get test resource data"); }
        File f = new File(url.toURI());
        if(!f.exists()){ throw new RuntimeException("Can't find test data: " + f); };
        String json = new java.util.Scanner(f, "UTF-8").useDelimiter("\\Z").next();
        System.out.println("Read test input json as: " + json);
        return json;
    }
}
