package dmeeuwis.nakama.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

public abstract class DataHelper {

    public abstract SQLiteDatabase getWritableDatabase();

    public List<Map<String, String>> selectRecords(String sql, Object ... params){
    	List<Map<String, String>> result = null;
    	String[] sparams = asStringArray(params);
    	Cursor c = this.getWritableDatabase().rawQuery(sql,sparams);
    	try {
	        if(c != null && c.moveToFirst()){
	        	result = new ArrayList<Map<String, String>>(c.getCount());
	        	int columnCount = c.getColumnCount();
	        	
	        	Map<String, String> m = new HashMap<String, String>();
	        	for(int i = 0; i < columnCount; i++){
	        		String colName = c.getColumnName(i);
	        		String rowColValue = c.getString(i);
	        		m.put(colName, rowColValue);
	        	}
	        	result.add(m);
	        }
    	} finally {
    		if(c != null) c.close();
    	}
    	return result;
    }
    
    public Integer selectInteger(String sql, Object ... params){
    	Cursor c = this.getWritableDatabase().rawQuery(sql, asStringArray(params));
    	try {
	        if(c.moveToFirst())
	        	return c.getInt(0);
    	} finally {
    		c.close();
    	}
    	throw new SQLiteException("Could not find id from newly created vocab list.");
    }

    public String selectString(String sql, Object ... params){
    	Cursor c = this.getWritableDatabase().rawQuery(sql, asStringArray(params));
    	try {
	        if(c.moveToFirst())
	        	return c.getString(0);
    	} finally {
    		c.close();
    	}
    	throw new SQLiteException("Could not find id from newly created vocab list.");
    }
    
    public String selectStringOrNull(String sql, Object ... params){
    	Cursor c = this.getWritableDatabase().rawQuery(sql, asStringArray(params));
    	try {
	        if(c.moveToFirst())
	        	return c.getString(0);
    	} finally {
    		c.close();
    	}
    	return null;
    }
    
    public List<String> selectStringList(String sql, Object ... params){
    	List<String> entries = new LinkedList<String>();
    	Cursor c =  this.getWritableDatabase().rawQuery(sql, asStringArray(params));
    	try {
	    	if(c.moveToFirst()){
	    		do {
	    			String value = c.getString(0);
	    			entries.add(value);
	    		} while(c.moveToNext());
	    	}
    	} finally {
    		c.close();
    	}
    	return entries;
    }
    
    public List<Integer> selectIntegerList(String sql, Object ... params){
    	List<Integer> entries = new LinkedList<Integer>();
    	Cursor c =  this.getWritableDatabase().rawQuery(sql, asStringArray(params));
    	try {
	    	if(c.moveToFirst()){
	    		do {
	    			Integer value = c.getInt(0);
	    			entries.add(value);
	    		} while(c.moveToNext());
	    	}
    	} finally {
    		c.close();
    	}
    	return entries;
    }

    
    
    
    
    private String[] asStringArray(Object[] params){
    	String[] sparams = new String[params.length];
    	for(int i = 0; i < params.length; i++){
    		sparams[i] = params[i].toString();
    	}
    	return sparams;
    }
    
    public class DataOpenHelper extends SQLiteOpenHelper {
    	private static final String DB_NAME = "vocab_lists.db";
    	private static final int DB_VERSION = 1;
    	
    	public DataOpenHelper(Context context) {
    		super(context, DB_NAME, null, DB_VERSION);
    	}

    	@Override
    	public void onCreate(SQLiteDatabase dbase) {
    		dbase.execSQL("CREATE TABLE vocab_lists ( " +
    				"list_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
    				"name TEXT NOT NULL " +
    			")"
    		);
    		
    		dbase.execSQL("CREATE TABLE vocab_list_entries (" +
    				"list_id INTEGER NOT NULL, " +
    				"edict_id INTEGER  NOT NULL, " +

    				"PRIMARY KEY(list_id, edict_id) " +
    			")"
    		  );
    	}
    	
    	@Override
    	public void onUpgrade(SQLiteDatabase dbase, int oldVersion, int newVersion) {
    		// TODO Auto-generated method stub
    	}	
    }    
}