package dmeeuwis.kanjimaster.ui.billing;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import dmeeuwis.kanjimaster.logic.data.CharacterStudySet;

public abstract class LockChecker implements dmeeuwis.kanjimaster.logic.LockChecker {
	static final String GOOGLE_PLAY_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAu/V3i9u59KOUHFYLUch6MDhSIqrRmj44iQNf5zIwlldj+3oL4QNeB0xI44XgKW/D4Uomg/dma0zQqfWMqen1BjAdt9bXyoSaGbHy8sPPMGrZqbAagz59ms2PzyP+o/Y+FEr2/OAsUxBG9CMUCo1cM4YktNDNQ5wRUXTURLmW4b9bhxksX/PFEZFmGA8wH5eHAJFTlnOUmVqsCePVgh6mKBxublfi9xwrQlHYReVbX05whRb8UI8UCZpKQasYbeskwbYGw61F0Z6K3TNAlip+20Ad18rH2VoBHxM5RXnItx+GBPE3f/Uj3QUsshD09IuqSpapl344f9pNUS+yiq/XqwIDAQAB";
	static final String LICENSE_SKU = "write_japanese_unlock"; // "android.test.purchased";
	static final int REQUEST_CODE = 837;

	private static final String PREFS_KEY = "unlockKey";

	final FragmentActivity parentActivity;

    abstract public boolean handleActivityResult(int requestCode, int resultCode, Intent data);
	abstract public void dispose();

	public LockChecker(FragmentActivity parent){
		this.parentActivity = parent;
	}

	@Override
    public void coreLock(){
		Log.d("nakama", "LockChecker: coreLock");

		SharedPreferences prefs = getSharedPrefs();
		SharedPreferences.Editor ed = prefs.edit();
		ed.remove(PREFS_KEY);
		ed.apply();
	}

	@Override
    public void coreUnlock(){
		Log.d("nakama", "LockChecker: coreUnlock");

		SharedPreferences prefs = getSharedPrefs();
		SharedPreferences.Editor ed = prefs.edit();
		ed.putString(PREFS_KEY, unlockKey());
		ed.apply();
	}

	protected SharedPreferences getSharedPrefs(){
		return PreferenceManager.getDefaultSharedPreferences(parentActivity.getApplicationContext());
	}

	@Override
    public CharacterStudySet.LockLevel getPurchaseStatus(){
		SharedPreferences prefs = getSharedPrefs();
		return getPurchaseStatus(prefs);
	}

	public static CharacterStudySet.LockLevel getPurchaseStatus(SharedPreferences prefs){
		String unlocked = prefs.getString(PREFS_KEY, null);
		// Log.d("nakama", "LockChecker: Unlock key from SharedPreferences: " + unlocked);
		if(unlocked != null){
			if(unlocked.equals(unlockKey())){
				// Log.d("nakama", "LockChecker: getPurchaseStatus: Unlock key matched: UNLOCKED");
				return CharacterStudySet.LockLevel.UNLOCKED;
			}
		}
		// Log.d("nakama", "LockChecker: getPurchaseStatus: Unlock key didn't match: LOCKED");
		return CharacterStudySet.LockLevel.LOCKED;

	}

	static String unlockKey(){
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
		String id = Settings.Secure.ANDROID_ID != null ? Settings.Secure.ANDROID_ID : "alkdjsklj9q90adsadsa0";
		byte[] bytes = (id + "_WRITE_JAPANESE").getBytes();
		md.update(bytes, 0, bytes.length);
		byte[] digest = md.digest();
		return bytesToHex(digest);
	}

	// http://stackoverflow.com/questions/9655181/convert-from-byte-array-to-hex-string-in-java
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	private static String bytesToHex( byte[] bytes ) {
		char[] hexChars = new char[ bytes.length * 2 ];
		for( int j = 0; j < bytes.length; j++ ) {
			int v = bytes[ j ] & 0xFF;
			hexChars[ j * 2 ] = hexArray[ v >>> 4 ];
			hexChars[ j * 2 + 1 ] = hexArray[ v & 0x0F ];
		}
		return new String( hexChars );
	}

	public FragmentActivity getParentActivity(){
		return parentActivity;
	}
}
