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
	private final String PRIVACY_POLICY = "<h1>Write Japanese Privacy Policy</h1>" +
		"<p>Write Japanese uses some personal information to directly support the app's functionality. It is never given to third parties, and never used for marketing.</p>" +
		"<p>If the user opts into multi-device sync, then the user's google account email will be used to sync between the users devices. This will mean the the email address will be uploaded to the Write Japanese server at dmeeuwis.com. It will be used only for the purpose of syncing data across devices, and never for any other purpose. It will never be given away to third parties. If the user does not opt into multi-device sync, then this information is never requested or transmitted.</p>" +
		"<p>If the opts into Story Sharing, users' character stories will be transmitted to the server at dmeeuwis.com, and shared with other users. No personally identifiable information is used, or transmitted to other users. If the Story Sharing option is not opted into, then no information is sent.</p>" +
		"<p>If the user opts into reporting grading overrides, then the users drawing data and grading result will be sent ot the server at dmeeuwis.com. If the user does not opt in, then this data is never transmitted.</p>" +
		"<p>In the event of an application crash, some forensics data is sent to the server at dmeeuwis.com. The crash data does not include personally identifiable information.</p>" +
		"<p>The app owner is David Meeuwis, reachable at dmeeuwis@gmail.com.</p>";


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
