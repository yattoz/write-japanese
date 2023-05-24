package dmeeuwis.kanjimaster.logic.data;

public class StoryDataHelper {

    public void recordStory(char character, String story){
        String existing = getStory(character);
        if (existing == null) {
            DataHelperFactory.get().execSQL("INSERT INTO kanji_stories(character, story, timestamp) VALUES(?, ?, CURRENT_TIMESTAMP)",
                    new String[]{Character.toString(character), story});
        } else {
            DataHelperFactory.get().execSQL("UPDATE kanji_stories SET story = ?, timestamp = CURRENT_TIMESTAMP WHERE character = ?",
                    new String[]{story, String.valueOf(character)});

        }
    }
    
    public String getStory(char character){
        return DataHelperFactory.get().selectStringOrNull("SELECT story FROM kanji_stories WHERE character = ?", Character.toString(character));
	}
}
