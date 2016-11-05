package dmeeuwis.nakama.views.translations;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;
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
	}
	
	@Override
	protected Void doInBackground(Void... params) {
		Thread.currentThread().setName("TranslationSearchAsyncTask");
		try {
			doTranslationsLoop();
			return null;

		} catch(ClosedByInterruptException cex){
			Log.e("nakama", "ClosedByInterruptException, retrying DictionarySet translations", cex);
			try {
				doTranslationsLoop();
				return null;
			} catch(Throwable e){
				UncaughtExceptionLogger.logError(Thread.currentThread(), "nakama", e, null);
				return null;
			}
		} catch(Throwable e){
			UncaughtExceptionLogger.logError(Thread.currentThread(), "nakama", e, null);
			return null;
		}
	}

	private void doTranslationsLoop() throws IOException, XmlPullParserException {
		int translationIndex = 0;
		final int BATCH_SIZE = 1;
		DictionarySet dictSet = new DictionarySet(context);
		try {
			List<Translation> nextBatch;
			do {
				nextBatch = dictSet.querier.orQueries(translationIndex, BATCH_SIZE, this.query);
				translationIndex += BATCH_SIZE;

				List<Translation> accepted = new ArrayList<Translation>(nextBatch.size());
				for (Translation t : nextBatch) {
					Translation restricted = t.restrictToCommonKanjiElements(kanji);
					if (restricted != null) {
						accepted.add(restricted);
					}
				}

				publishProgress(accepted.toArray(new Translation[0]));
			}
			while (nextBatch.size() <= BATCH_SIZE && nextBatch.size() > 0 && translationIndex < MAX_TRANSLATIONS && !this.isCancelled());
			Log.i("nakama", "Finished background translation work");
		} finally {
			dictSet.close();
		}

	}
	
	@Override
	protected void onProgressUpdate(Translation... params){
		for(Translation t: params){
			adder.add(t);
		}
	}
}