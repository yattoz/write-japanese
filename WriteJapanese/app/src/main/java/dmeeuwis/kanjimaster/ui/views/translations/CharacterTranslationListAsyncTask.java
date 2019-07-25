package dmeeuwis.kanjimaster.ui.views.translations;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.util.Log;

import java.io.InputStream;

import dmeeuwis.kanjimaster.core.Translation;
import dmeeuwis.kanjimaster.logic.data.TranslationsFromXml;
import dmeeuwis.kanjimaster.logic.data.UncaughtExceptionLogger;

public class CharacterTranslationListAsyncTask extends AsyncTask<Void, Translation, Void> {
	final private Context context;
	final private AddTranslation adder;
	final private char kanji;

	public interface AddTranslation {
		void add(Translation t);
	}

	public CharacterTranslationListAsyncTask(AddTranslation adder, Context context, char kanji){
		this.context = context;
		this.kanji = kanji;
		this.adder = adder;

		Log.i("nakama", "Starting background vocab translation for " + kanji);
	}
	
	@Override
	protected Void doInBackground(Void... params) {
		Thread.currentThread().setName("TranslationSearchAsyncTask");
        Log.d("nakama-vocab", "Starting vocab reader thread against " + kanji + "...");
        try {
            AssetManager am = context.getAssets();
            String filename = Integer.toHexString(((Character)kanji).charValue());
            InputStream in = am.open("char_vocab/" + filename + "_trans.xml");

            TranslationsFromXml.PublishTranslation p = new TranslationsFromXml.PublishTranslation() {
                @Override
                public void publish(Translation t) {
                    publishProgress(t);
                }
            };
            TranslationsFromXml txml = new TranslationsFromXml();
            txml.load(in, p);

        } catch(Throwable e){
            Log.e("nakama", "Saw error reading translations: " + e, e);
            if(this.isCancelled()) {
                Log.d("nakama", "Caught exception in translation background thread, but isCancelled anyways", e);
            } else {
                UncaughtExceptionLogger.backgroundLogError("Error during (non-cancelled) background translation", e);
            }
        }
        Log.i("nakama", "Completed background translation work for " + kanji);
        return null;
	}

	@Override
	protected void onProgressUpdate(Translation... params){
		for(Translation t: params){
			adder.add(t);
		}
	}
}