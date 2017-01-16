package dmeeuwis.nakama.data;

import android.content.Context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        WriteJapaneseOpenHelper db = new WriteJapaneseOpenHelper(context);
        try {
            db.getWritableDatabase().execSQL("INSERT INTO character_set_edits(id, name, description, set, install_id, timestamp) VALUES(?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                    new String[]{id, name, desc, set, Iid.get(context).toString()});
        } finally {
            db.close();
        }
    }

    public List<CharacterStudySet> getSets() {
        WriteJapaneseOpenHelper db = new WriteJapaneseOpenHelper(context);
        Map<String, CharacterStudySet> sets = new LinkedHashMap<>();
        try {
            List<Map<String, String>> records =
                DataHelper.selectRecords(db.getReadableDatabase(),
                        "SELECT id, name, description, characters FROM character_set_edits ORDER BY timestamp");

            for(Map<String, String> r: records){
                CharacterStudySet s = new CharacterStudySet(r.get("name"), r.get("name"), r.get("description"),
                        r.get("id"), CharacterStudySet.LockLevel.UNLOCKED, r.get("set"), r.get("set"), null, Iid.get(context), false);
                sets.put(r.get("id"), s);
            }
        } finally {
            db.close();
        }
        return new ArrayList<>(sets.values());
    }
}
