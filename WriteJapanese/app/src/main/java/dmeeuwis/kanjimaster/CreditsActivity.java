package dmeeuwis.kanjimaster;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

public class CreditsActivity extends ActionBarActivity {

	private final String EDICT_THANKS = "<p>This software uses the EDICT and KANJIDIC dictionary files. These files are the property of the Electronic Dictionary Research and Development Group, and are used in conformance with the Group's licence. For further information, see <a href='http://www.edrdg.org/'>the Electronic Dictionary Research and Development Group homepage</a></p>";
	private final String DIAGRAMS_THANKS = "<p>This software also uses the KanjiVG project's SVG files that describe the shape of each kanji. The KanjiVG is copyright © 2009-2013 Ulrich Apel and released under the Creative Commons Attribution-Share Alike 3.0 license. For more information, see the <a href='http://kanjivg.tagaini.net/'>KanjiVG homepage.</p>";

	String callingClass;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_credits);
		
    	ActionBar actionBar = this.getSupportActionBar();
    	actionBar.setDisplayHomeAsUpEnabled(true);
    	
    	Bundle params = getIntent().getExtras();
    	callingClass = params.getString("parent");

		TextView thanks = (TextView)findViewById(R.id.text_space);
		thanks.setClickable(true);
		thanks.setMovementMethod(LinkMovementMethod.getInstance());
		thanks.setText(Html.fromHtml(EDICT_THANKS + DIAGRAMS_THANKS));
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.d("nakama", "ActionBar: in onOptionsItemSelected: " + item.toString());
	
		if(item.getItemId() == android.R.id.home){
			try {
				Intent backUp = new Intent(this, Class.forName(callingClass));
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
