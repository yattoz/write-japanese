package dmeeuwis.nakama.data;

import android.app.Activity;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

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

        PracticeLogSync.ExternalDependencies ep = new PracticeLogSync.ExternalDependencies(null){
            @Override
            public InputStream sendPost(String jsonPost) throws IOException {
                String json =
            }
        };
        PracticeLogSync sync = new PracticeLogSync()

    }
}
