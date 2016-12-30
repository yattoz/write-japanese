package dmeeuwis.nakama.views.translations;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import dmeeuwis.Translation;
import dmeeuwis.nakama.data.DictionarySet;
import dmeeuwis.nakama.data.UncaughtExceptionLogger;

public class KanjiTranslationListAsyncTask extends AsyncTask<Void, Translation, Void> {
	static final private int MAX_TRANSLATIONS = 20;
	final private Context context;
	final private String[] query;
	final private AddTranslation adder;
	final private char kanji;
	
	public interface AddTranslation {
		void add(Translation t);
	}
	
	public KanjiTranslationListAsyncTask(AddTranslation adder, Context context, char kanji){
		this.context = context;
		this.query = new String[] { kanji + ""};
		this.kanji = kanji;
		this.adder = adder;

		Log.i("nakama", "Starting background vocab translation for " + kanji);
	}
	
	@Override
	protected Void doInBackground(Void... params) {
		Thread.currentThread().setName("TranslationSearchAsyncTask");
		try {
			doTranslationsLoop();
			return null;

		} catch(Throwable e){
			UncaughtExceptionLogger.logError(Thread.currentThread(), "nakama", e, null);
			return null;
		}
	}

	private void doTranslationsLoop() throws IOException, XmlPullParserException {
		int translationIndex = 0;
		final int BATCH_SIZE = 1;

        List<Translation> nextBatch;
		DictionarySet dictSet = null;
		try {
			dictSet = new DictionarySet(context);
			do {
				nextBatch = dictSet.querier.singleCharacterSearch(BATCH_SIZE, translationIndex, Character.valueOf(kanji));
				translationIndex += BATCH_SIZE;

				List<Translation> accepted = new ArrayList<Translation>(nextBatch.size());
				for (Translation t : nextBatch) {
					Translation restricted = t.restrictToCommonKanjiElements(kanji);
					if (restricted != null && restricted.toKanjiString().contains(String.valueOf(kanji))) {
						accepted.add(restricted);
					}
				}

				publishProgress(accepted.toArray(new Translation[0]));

				if (this.isCancelled()) {
					Log.i("nakama", "Translation background task for " + kanji + " is cancelled, ending.");
					return;
				}
			}
			while (nextBatch.size() <= BATCH_SIZE && nextBatch.size() > 0 && translationIndex < MAX_TRANSLATIONS);
		} catch(Throwable e){
			if(this.isCancelled()) {
				Log.d("nakama", "Caught exception in translation background thread, but isCancelled anyways", e);
			} else {
				UncaughtExceptionLogger.backgroundLogError("Error during (non-cancelled) background translation", e, context);
			}
		} finally {
			if(dictSet != null) {
				dictSet.close();
			}
		}
        Log.i("nakama", "Completed background translation work for " + kanji);

	}
	
	@Override
	protected void onProgressUpdate(Translation... params){
		for(Translation t: params){
			adder.add(t);
		}
	}
}