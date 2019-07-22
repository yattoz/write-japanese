package dmeeuwis.kanjimaster.ui.data;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.util.Log;

import org.xmlpull.v1.XmlPullParserException;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import dmeeuwis.kanjimaster.core.Translation;
import dmeeuwis.kanjimaster.core.indexer.KanjiFinder;
import dmeeuwis.kanjimaster.logic.data.TranslationsFromXml;

public class DictionarySet {

	public static final String KANJIDICT_FILE = "kanjidic.utf8.awb";
	public static final String KANJIDICT_INDEX = "kanjidic.index.awb";

	private KanjiFinder kanjiFinder;

	final AssetManager asm;

	final AssetFileDescriptor kanjiDictFd;
	final FileInputStream kanjiDictStream;
	final FileChannel kanjiDictCh;
	
	final AssetFileDescriptor kanjiIndexFd;
	final FileInputStream kanjiIndexStream;

    private static DictionarySet singleton;
    public static synchronized DictionarySet get(Context context){
        if(singleton != null){
            return singleton;
        }
        singleton = new DictionarySet(context);
        return singleton;
    }

    public DictionarySet(Context context) {
	   	long start = System.currentTimeMillis();
	   	asm = context.getAssets();
        
		try {
			kanjiDictFd = asm.openFd(KANJIDICT_FILE);
			kanjiDictStream = this.kanjiDictFd.createInputStream();
			kanjiDictCh = kanjiDictStream.getChannel();
			
	        kanjiIndexFd = asm.openFd(KANJIDICT_INDEX);
	        kanjiIndexStream = this.kanjiIndexFd.createInputStream();

			Log.i("nakama", "DictionarySet: Time to ready DictionarySet: " + (System.currentTimeMillis() - start) + "ms");

		} catch(IOException e){
			throw new RuntimeException("Error accessing internal assets: " + e.getMessage(), e);
		}
	}

	synchronized public KanjiFinder kanjiFinder(){
		if(kanjiFinder == null){
			long kanjiDictOffset = kanjiDictFd.getStartOffset();
			try {
				kanjiFinder = new KanjiFinder(kanjiIndexStream, kanjiIndexFd.getLength(), kanjiDictCh, kanjiDictOffset);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} 
		return kanjiFinder;
	}

	public List<Translation> loadTranslations(Character kanji, int limit) throws IOException, XmlPullParserException {
		String filename = Integer.toHexString(((Character)kanji).charValue());
		InputStream in = asm.open("char_vocab/" + filename + "_trans.xml");
		final List<Translation> collect = new ArrayList<>();
		TranslationsFromXml.PublishTranslation p = new TranslationsFromXml.PublishTranslation() {
			@Override
			public void publish(Translation t) {
				collect.add(t);
			}
		};
		TranslationsFromXml t = new TranslationsFromXml();
		t.load(in, p, limit);
		return collect;
	}

	public void close(){
		safeClose(kanjiDictStream);
		safeClose(kanjiDictFd);
        safeClose(kanjiDictCh);
		
		safeClose(kanjiIndexStream);
		safeClose(kanjiIndexFd);
	}
    private static void safeClose(Closeable c){
        if(c != null){
            try {
                c.close();
            } catch(Throwable t){
                // ignore
            }
        }
    }

	private static void safeClose(AssetFileDescriptor c){
		if(c != null){
			try {
				c.close();
			} catch(Throwable t){
				// ignore
			}
		}
	}
}
