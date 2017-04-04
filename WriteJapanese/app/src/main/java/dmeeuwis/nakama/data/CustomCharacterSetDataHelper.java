package dmeeuwis.nakama.data;

import android.content.Context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import dmeeuwis.nakama.primary.Iid;

public class CustomCharacterSetDataHelper {
    private final Context context;

    public CustomCharacterSetDataHelper(Context c) {
        if (c == null) {
            throw new IllegalArgumentException("Need a not-null context to construct StoryDataHelper");
        }
        this.context = c;
    }

    public void recordEdit(String id, String name, String desc, String set) {
        recordRemoteEdit(UUID.randomUUID().toString(), id, name, desc, set, Iid.get(context).toString(), null, Boolean.FALSE);
    }

    void recordRemoteEdit(String editId, String charsetId, String name, String desc, String set, String installId, String timestamp, Boolean deleted) {
        WriteJapaneseOpenHelper db = new WriteJapaneseOpenHelper(context);
        try {
            db.getWritableDatabase().execSQL("INSERT INTO character_set_edits(id, charset_id, name, description, characters, install_id, timestamp, deleted) VALUES(?, ?, ?, ?, ?, ?, COALESCE(?, CURRENT_TIMESTAMP, deleted))",
                    new Object[]{editId, charsetId, name, desc, set, installId, timestamp, deleted});
        } finally {
            db.close();
        }
    }

    public void delete(CharacterStudySet c){
        WriteJapaneseOpenHelper db = new WriteJapaneseOpenHelper(context);
        try {
            db.getWritableDatabase().execSQL("INSERT INTO character_set_edits(id, charset_id, name, description, characters, install_id, timestamp, deleted) VALUES(?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?)",
                    new Object[]{ UUID.randomUUID().toString(), c.pathPrefix, c.name, c.description, c.charactersAsString(), Iid.get(context).toString(), Boolean.TRUE});
        } finally {
            db.close();
        }
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
        WriteJapaneseOpenHelper db = new WriteJapaneseOpenHelper(context);
        Map<String, CharacterStudySet> sets = new LinkedHashMap<>();
        try {
            List<Map<String, String>> records =
                DataHelper.selectRecords(db.getReadableDatabase(),
                        "SELECT charset_id, name, description, characters, deleted FROM character_set_edits ORDER BY timestamp");

            for(Map<String, String> r: records){
                if(r.get("deleted").equals("1")){
                    sets.remove(r.get("charset_id"));
                } else {
                    CharacterStudySet s = new CharacterStudySet(r.get("name"), r.get("name"), r.get("description"),
                            r.get("charset_id"), CharacterStudySet.LockLevel.UNLOCKED, r.get("characters"), r.get("characters"), null, Iid.get(context), false, context);
                    sets.put(r.get("charset_id"), s);
                }
            }
        } finally {
            db.close();
        }
        return new ArrayList<>(sets.values());
    }
}
