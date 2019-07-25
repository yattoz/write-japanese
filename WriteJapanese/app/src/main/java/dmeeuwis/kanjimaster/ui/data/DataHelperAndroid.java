package dmeeuwis.kanjimaster.ui.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import dmeeuwis.kanjimaster.logic.data.DataHelper;
import dmeeuwis.kanjimaster.logic.util.JsonWriter;

public class DataHelperAndroid implements DataHelper {

   private Context context;

    public DataHelperAndroid(Context ctx){
        this.context = ctx;
    }

    public Map<String, Map<String, String>> selectRecordsIndexedByFirst(String sql, String indexKey, Object ... params) {
        List<Map<String, String>> results = selectRecords(sql, params);
        Map<String, Map<String, String>> indexed = new LinkedHashMap<>();
        for(Map<String, String> r: results){
            indexed.put(r.get(indexKey), r);
        }
        return indexed;
    }

    public Map<String, String> selectRecord(String sql, Object... params){
        List<Map<String, String>> out = selectRecords(sql, params);
        if(out.size() == 0){ return null; }
        if(out.size() > 1){
            throw(new RuntimeException("Bad query; selectRecord called, but multiple rows returned. Query was:\n" + sql));
        }

        return out.get(0);
    }

    @Override
    public void execSQL(String s, String[] strings) {
        WriteJapaneseOpenHelper woh = new WriteJapaneseOpenHelper(context);
        SQLiteDatabase db = woh.getWritableDatabase();
        db.execSQL(s, strings);
    }

    @Override
    public void execSQL(String s) {
        WriteJapaneseOpenHelper woh = new WriteJapaneseOpenHelper(context);
        SQLiteDatabase db = woh.getWritableDatabase();
        db.execSQL(s);
    }

    interface ProcessRow {
        void process(Map<String, String> row);
    }


    public int applyToResults(ProcessRow rowProcessor, String sql, Object ... params){
        WriteJapaneseOpenHelper woh = new WriteJapaneseOpenHelper(this.context);
        SQLiteDatabase db = woh.getReadableDatabase();

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


    public List<Map<String, String>> selectRecords(String sql, Object ... params){
        WriteJapaneseOpenHelper woh = new WriteJapaneseOpenHelper(this.context);
        SQLiteDatabase db = woh.getReadableDatabase();

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

    public Integer selectInteger(String sql, Object ... params){
        WriteJapaneseOpenHelper woh = new WriteJapaneseOpenHelper(this.context);
        SQLiteDatabase db = woh.getReadableDatabase();

        Cursor c = db.rawQuery(sql, asStringArray(params));
        try {
            if(c.moveToFirst())
                return c.getInt(0);
        } finally {
            c.close();
        }
        throw new RuntimeException("Could not find id from newly created vocab list.");
    }

    public String selectString(String sql, Object ... params){
        WriteJapaneseOpenHelper woh = new WriteJapaneseOpenHelper(this.context);
        SQLiteDatabase db = woh.getReadableDatabase();

        Cursor c = db.rawQuery(sql, asStringArray(params));
        try {
            if(c.moveToFirst())
                return c.getString(0);
        } finally {
            c.close();
        }
        throw new RuntimeException("Could not find id from newly created vocab list.");
    }

    public String selectStringOrNull(String sql, Object ... params){
        WriteJapaneseOpenHelper woh = new WriteJapaneseOpenHelper(this.context);
        SQLiteDatabase db = woh.getReadableDatabase();

        Cursor c = db.rawQuery(sql, asStringArray(params));
        try {
            if(c.moveToFirst())
                return c.getString(0);
        } finally {
            c.close();
        }
        return null;
    }

    public List<String> selectStringList(String sql, Object ... params){
        WriteJapaneseOpenHelper woh = new WriteJapaneseOpenHelper(this.context);
        SQLiteDatabase db = woh.getReadableDatabase();

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

    public List<Integer> selectIntegerList(String sql, Object ... params){
        WriteJapaneseOpenHelper woh = new WriteJapaneseOpenHelper(this.context);
        SQLiteDatabase db = woh.getReadableDatabase();

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

    public String[] asStringArray(Object[] params){
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

    @Override
    public void queryToJsonArray(String name, String sql, String[] args, JsonWriter jw) throws IOException {
        WriteJapaneseOpenHelper woh = new WriteJapaneseOpenHelper(context);
        SQLiteDatabase sqlite = woh.getReadableDatabase();
        Cursor c = sqlite.rawQuery(sql, args);
        try {
            // stream over standardSets rows since that time
            jw.name(name);
            jw.beginArray();
            while (c.moveToNext()) {
                jw.beginObject();
                for (int i = 0; i < c.getColumnCount(); i++) {
                    jw.name(c.getColumnName(i));
                    jw.value(c.getString(i));
                }
                jw.endObject();
            }
        } finally {
            c.close();
        }
        jw.endArray();
    }

}

