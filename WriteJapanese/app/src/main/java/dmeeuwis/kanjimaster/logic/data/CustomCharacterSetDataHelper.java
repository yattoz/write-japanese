package dmeeuwis.kanjimaster.logic.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import dmeeuwis.kanjimaster.ui.sections.primary.Iid;

public class CustomCharacterSetDataHelper {
    public void recordEdit(String id, String name, String desc, String set) {
        recordRemoteEdit(UUID.randomUUID().toString(), id, name, desc, set, Iid.get().toString(), null, Boolean.FALSE);
    }

    void recordRemoteEdit(String editId, String charsetId, String name, String desc, String set, String installId, String timestamp, Boolean deleted) {
        DataHelperFactory.get().execSQL(
                "INSERT INTO character_set_edits(id, charset_id, name, description, characters, install_id, timestamp, deleted) VALUES(?, ?, ?, ?, ?, ?, COALESCE(?, CURRENT_TIMESTAMP), ?)",
                new String[]{editId, charsetId, name, desc, set, installId, timestamp, deleted.toString()});
    }

    public void delete(CharacterStudySet c){
        DataHelperFactory.get().execSQL(
                "INSERT INTO character_set_edits(id, charset_id, name, description, characters, install_id, timestamp, deleted) VALUES(?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?)",
                new String[]{ UUID.randomUUID().toString(), c.pathPrefix, c.name, c.description, c.charactersAsString(), Iid.get().toString(), Boolean.TRUE.toString()});
    }

    public void unDelete(CharacterStudySet doomed) {
        recordEdit(doomed.pathPrefix, doomed.name, doomed.description, doomed.charactersAsString());
    }

    public CharacterStudySet get(String id){
        List<CharacterStudySet> customs = getSets();
        for(CharacterStudySet s: customs){
            if(s.pathPrefix.equals(id)){
                return s;
            }
        }
        return null;
    }

    public List<CharacterStudySet> getSets() {
        Map<String, CharacterStudySet> sets = new LinkedHashMap<>();
        List<Map<String, String>> records =
            DataHelperFactory.get().selectRecords(
                    "SELECT charset_id, name, description, characters, deleted FROM character_set_edits ORDER BY timestamp");

        for(Map<String, String> r: records){
            if(r.get("deleted").equals("1")){
                sets.remove(r.get("charset_id"));
            } else {
                CharacterStudySet s = new CharacterStudySet(r.get("name"), r.get("name"), r.get("description"),
                        r.get("charset_id"), CharacterStudySet.LockLevel.UNLOCKED, r.get("characters"), r.get("characters"), null, Iid.get(), false);
                sets.put(r.get("charset_id"), s);
            }
        }
        return new ArrayList<>(sets.values());
    }
}
