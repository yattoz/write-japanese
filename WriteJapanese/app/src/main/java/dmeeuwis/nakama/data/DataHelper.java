package dmeeuwis.nakama.data;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DataHelper {

	public static Map<String, String> selectRecord(SQLiteDatabase db, String sql, Object ... params){
		List<Map<String, String>> out = selectRecords(db, sql, params);
		if(out.size() == 0){ return null; }
		if(out.size() > 1){
			throw(new RuntimeException("Bad query; selectRecord called, but multiple rows returned. Query was:\n" + sql));
		}

		return out.get(0);
	}

	public static Map<String, Map<String, String>> selectRecordsIndexedByFirst(SQLiteDatabase db, String sql, String indexKey, Object ... params) {
		List<Map<String, String>> results = selectRecords(db, sql, params);
		Map<String, Map<String, String>> indexed = new LinkedHashMap<>();
		for(Map<String, String> r: results){
			indexed.put(r.get(indexKey), r);
		}
		return indexed;
	}

	interface ProcessRow {
		void process(Map<String, String> row);
	}


	public static int applyToResults(ProcessRow rowProcessor, SQLiteDatabase db, String sql, Object ... params){
		String[] sparams = asStringArray(params);
		Cursor c = db.rawQuery(sql,sparams);
		int count = 0;
		try {
			int columnCount = c.getColumnCount();
			while(c.moveToNext()){
				count++;
				Map<String, String> m = new HashMap<String, String>();
				for(int i = 0; i < columnCount; i++){
					String colName = c.getColumnName(i);
					String rowColValue = c.getString(i);
					m.put(colName, rowColValue);
				}
				rowProcessor.process(m);
			}
		} finally {
			if(c != null) c.close();
		}
		return count;
	}


    public static List<Map<String, String>> selectRecords(SQLiteDatabase db, String sql, Object ... params){
    	List<Map<String, String>> result;
    	String[] sparams = asStringArray(params);
    	Cursor c = db.rawQuery(sql,sparams);
    	try {
            result = new ArrayList<>(c.getCount());
            int columnCount = c.getColumnCount();
            while(c.moveToNext()){
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

    public static Integer selectInteger(SQLiteDatabase db, String sql, Object ... params){
    	Cursor c = db.rawQuery(sql, asStringArray(params));
    	try {
	        if(c.moveToFirst())
	        	return c.getInt(0);
    	} finally {
    		c.close();
    	}
    	throw new SQLiteException("Could not find id from newly created vocab list.");
    }

    public static String selectString(SQLiteDatabase db, String sql, Object ... params){
    	Cursor c = db.rawQuery(sql, asStringArray(params));
    	try {
	        if(c.moveToFirst())
	        	return c.getString(0);
    	} finally {
    		c.close();
    	}
    	throw new SQLiteException("Could not find id from newly created vocab list.");
    }

    public static String selectStringOrNull(SQLiteDatabase db, String sql, Object ... params){
    	Cursor c = db.rawQuery(sql, asStringArray(params));
    	try {
	        if(c.moveToFirst())
	        	return c.getString(0);
    	} finally {
    		c.close();
    	}
    	return null;
    }

    public static List<String> selectStringList(SQLiteDatabase db, String sql, Object ... params){
    	List<String> entries = new LinkedList<String>();
    	Cursor c =  db.rawQuery(sql, asStringArray(params));
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

    public static List<Integer> selectIntegerList(SQLiteDatabase db, String sql, Object ... params){
    	List<Integer> entries = new LinkedList<Integer>();
    	Cursor c =  db.rawQuery(sql, asStringArray(params));
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

    private static String[] asStringArray(Object[] params){
    	String[] sparams = new String[params.length];
    	for(int i = 0; i < params.length; i++){
            if(params[i] == null){
                sparams[i] = null;
            } else {
                sparams[i] = params[i].toString();
            }
    	}
    	return sparams;
    }
}