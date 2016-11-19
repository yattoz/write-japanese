package dmeeuwis.nakama;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import dmeeuwis.nakama.data.CharacterStudySet;

public abstract class ILockChecker {
	public static final String GOOGLE_PLAY_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAu/V3i9u59KOUHFYLUch6MDhSIqrRmj44iQNf5zIwlldj+3oL4QNeB0xI44XgKW/D4Uomg/dma0zQqfWMqen1BjAdt9bXyoSaGbHy8sPPMGrZqbAagz59ms2PzyP+o/Y+FEr2/OAsUxBG9CMUCo1cM4YktNDNQ5wRUXTURLmW4b9bhxksX/PFEZFmGA8wH5eHAJFTlnOUmVqsCePVgh6mKBxublfi9xwrQlHYReVbX05whRb8UI8UCZpKQasYbeskwbYGw61F0Z6K3TNAlip+20Ad18rH2VoBHxM5RXnItx+GBPE3f/Uj3QUsshD09IuqSpapl344f9pNUS+yiq/XqwIDAQAB";
	public static final String LICENSE_SKU = "write_japanese_unlock"; // "android.test.purchased";
	public static final int REQUEST_CODE = 837;

	private static final String PREFS_KEY = "unlockKey";

	private final Activity parentActivity;

	abstract public void runPurchase();
	abstract public void startConsume();
	abstract public boolean handleActivityResult(int requestCode, int resultCode, Intent data);
	abstract public void dispose();

	public ILockChecker(Activity parent){
		this.parentActivity = parent;
	}

	public void coreLock(){
		Log.d("nakama", "IABLockChecker: coreLock");

		SharedPreferences prefs = getSharedPrefs();
		SharedPreferences.Editor ed = prefs.edit();
		ed.remove(PREFS_KEY);
		ed.apply();
	}

	public void coreUnlock(){
		Log.d("nakama", "IABLockChecker: coreUnlock");

		SharedPreferences prefs = getSharedPrefs();
		SharedPreferences.Editor ed = prefs.edit();
		ed.putString(PREFS_KEY, unlockKey());
		ed.apply();
	}

	private SharedPreferences getSharedPrefs(){
		return PreferenceManager.getDefaultSharedPreferences(parentActivity.getApplicationContext());
	}

	public CharacterStudySet.LockLevel getPurchaseStatus(){
		SharedPreferences prefs = getSharedPrefs();
		return getPurchaseStatus(prefs);
	}

	public static CharacterStudySet.LockLevel getPurchaseStatus(SharedPreferences prefs){
		String unlocked = prefs.getString(PREFS_KEY, null);
		// Log.d("nakama", "IABLockChecker: Unlock key from SharedPreferences: " + unlocked);
		if(unlocked != null){
			if(unlocked.equals(unlockKey())){
				// Log.d("nakama", "IABLockChecker: getPurchaseStatus: Unlock key matched: UNLOCKED");
				return CharacterStudySet.LockLevel.UNLOCKED;
			}
		}
		// Log.d("nakama", "IABLockChecker: getPurchaseStatus: Unlock key didn't match: LOCKED");
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

}
