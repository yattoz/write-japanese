package dmeeuwis.kanjimaster.ui.sections.credits;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import dmeeuwis.kanjimaster.BuildConfig;
import dmeeuwis.kanjimaster.R;
import dmeeuwis.kanjimaster.logic.data.IidFactory;
import dmeeuwis.kanjimaster.ui.sections.primary.KanjiMasterActivity;

public class CreditsActivity extends AppCompatActivity {

	private final String EDICT_THANKS = "<p>This software uses the EDICT, KANJIDIC, and kradfile-u dictionary files. These files are the property of the Electronic Dictionary Research and Development Group, and are used in conformance with the Group's licence. For further information, see <a href='http://www.edrdg.org/'>the Electronic Dictionary Research and Development Group homepage</a></p>";
	private final String DIAGRAMS_THANKS = "<p>This software also uses the KanjiVG project's SVG files that describe the shape of each kanji. The KanjiVG is copyright © 2009-2013 Ulrich Apel and released under the Creative Commons Attribution-Share Alike 3.0 license. For more information, see the <a href='http://kanjivg.tagaini.net/'>KanjiVG homepage.</p>";
	private final String PRIVACY_POLICY = "<p>The current version of the privacy policy for this app can be found <a href='https://dmeeuwis.com/write-japanese-privacy-policy.html'>here</a>.</p>";

	String callingClass;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_credits);
		
    	ActionBar actionBar = this.getSupportActionBar();
    	actionBar.setDisplayHomeAsUpEnabled(true);

		Intent intent = getIntent();
		if(intent != null) {
			Bundle params = getIntent().getExtras();
			if (params != null) {
				callingClass = params.getString("parent");
			}
		}

		TextView thanks = (TextView)findViewById(R.id.text_space);
		thanks.setClickable(true);
		thanks.setMovementMethod(LinkMovementMethod.getInstance());
		thanks.setText(Html.fromHtml(
				EDICT_THANKS +
				DIAGRAMS_THANKS +
				PRIVACY_POLICY +
				String.format("<div>Version: %s %d, code: %s</div>",
						BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE,
						IidFactory.get().toString())));
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.d("nakama", "ActionBar: in onOptionsItemSelected: " + item.toString());
	
		if(item.getItemId() == android.R.id.home){
			try {
				Intent backUp;
				if(callingClass != null) {
					backUp = new Intent(this, Class.forName(callingClass));
				} else {
					backUp = new Intent(this, KanjiMasterActivity.class);
				}
				startActivity(backUp);
			} catch (ClassNotFoundException e) {
				Log.e("nakama", "ClassNotFoundException while returning to parent. callingClass is " + callingClass, e);
				return false;
			} catch (Throwable t) {
				Log.e("nakama", "Throwable while returning to parent. callingClass is " + callingClass, t);
				return false;
			}
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
}
