package dmeeuwis.kanjimaster.logic.data;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import dmeeuwis.kanjimaster.logic.util.JsonWriter;

public interface DataHelper {

    String[] asStringArray(Object[] params);

    List<Integer> selectIntegerList(String sql, Object... params);
    List<String> selectStringList(String sql, Object... params);
    String selectStringOrNull(String sql, Object... params);
    String selectString(String sql, Object... params);
    Integer selectInteger(String sql, Object... params);
    List<Map<String, String>> selectRecords(String sql, Object... params);
    Map<String, Map<String, String>> selectRecordsIndexedByFirst(String sql, String indexKey, Object... params);
    Map<String, String> selectRecord(String sql, Object... params);

    void queryToJsonArray(String name, String sql, String[] args, JsonWriter jw) throws IOException;

    void execSQL(String s, String[] strings);
    void execSQL(String s);

    interface ProcessRow {
        int applyToResults(String sql, ProgressTracker pt, Object... params);
        void process(Map<String, String> row, ProgressTracker pt);
	}
}