package dmeeuwis.kanjimaster.logic.data;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;

import dmeeuwis.Translation;

public class TranslationsFromXml {

    public interface PublishTranslation {
        void publish(Translation t);
    }

    public void TranslationsFromXml(){ }


    public void load(InputStream in, PublishTranslation publisher) throws XmlPullParserException, IOException {
        load(in, publisher, Integer.MAX_VALUE);
    }

    public void load(InputStream in, PublishTranslation publisher, int limit) throws XmlPullParserException, IOException {

        XmlPullParserFactory parserFactory = XmlPullParserFactory.newInstance();
        parserFactory.setValidating(false);
        parserFactory.setNamespaceAware(false);
        XmlPullParser parser = parserFactory.newPullParser();
        parser.setInput(in, "UTF-8");

        // outer xml element
        parser.next();

        try {
            for(int i = 0; i < limit; i++){
                parser.next(); // outer-most entry tag: Translation expects to start inside it.
                Translation t = Translation.parseEdictXml(parser);
                publisher.publish(t);
            }
        } catch (NullPointerException e){
            // indicates end of file
        }
    }
}
