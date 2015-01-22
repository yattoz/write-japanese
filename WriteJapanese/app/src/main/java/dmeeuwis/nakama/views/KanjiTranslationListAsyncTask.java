package dmeeuwis.nakama.views;

import java.util.ArrayList;
import java.util.List;

import android.os.AsyncTask;
import android.util.Log;
import dmeeuwis.Translation;
import dmeeuwis.nakama.data.DictionarySet;
import dmeeuwis.util.Util;

public class KanjiTranslationListAsyncTask extends AsyncTask<Void, Translation, Void> {
	static final private int MAX_TRANSLATIONS = 20;
	final private String[] query;
	final private AddTranslation adder;
	final private DictionarySet dictSet;
	final private char kanji;
	
	public interface AddTranslation {
		public void add(Translation t);
	}
	
	public KanjiTranslationListAsyncTask(AddTranslation adder, DictionarySet dictSet, char kanji){
		this.query = new String[] { kanji + ""};
		this.dictSet = dictSet;
		this.kanji = kanji;
		this.adder = adder;
	}
	
	@Override
	protected Void doInBackground(Void... params) {
		int translationIndex = 0;
		final int BATCH_SIZE = 1;
		Thread.currentThread().setName("TranslationSearchAsyncTask");
		
		try {
			List<Translation> nextBatch;
			do {
				nextBatch = dictSet.querier.orQueries(translationIndex, BATCH_SIZE, this.query);
				translationIndex += BATCH_SIZE;
			
				List<Translation> accepted = new ArrayList<Translation>(nextBatch.size());
				for(Translation t: nextBatch){
					Translation restricted = t.restrictToCommonKanjiElements(kanji);
					if(restricted != null){
						accepted.add(restricted);
					}
				}
				
				publishProgress(accepted.toArray(new Translation[0]));
			} while(nextBatch.size() <= BATCH_SIZE && nextBatch.size() > 0 && translationIndex < MAX_TRANSLATIONS && !this.isCancelled());
				
			return null;
			
		} catch(Throwable e){
			Log.e("nakama", "Error caught from vocab background thread", e);
			return null;
		}
	}
	
	@Override
	protected void onProgressUpdate(Translation... params){
		for(Translation t: params){
			adder.add(t);
		}
	}
}