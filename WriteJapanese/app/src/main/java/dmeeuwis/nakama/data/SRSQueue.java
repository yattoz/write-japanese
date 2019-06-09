package dmeeuwis.nakama.data;

import android.os.*;
import android.util.*;

import org.jetbrains.annotations.NotNull;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.Period;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;

import dmeeuwis.util.Util;

public class SRSQueue {
    public static boolean useSRSGlobal = true;

    private PriorityQueue<SRSEntry> srsQueue;
    private final String id;

    public static final Period[] SRSTable = new Period[] {
            Period.ofDays(1),
            Period.ofDays(3),
            Period.ofDays(7),
            Period.ofDays(14),
            Period.ofDays(30)
    };

    static List<CharacterStudySet> globalSRSSets = new ArrayList<>();
    public static void registerSetsForGlobalSRS(Collection<CharacterStudySet> sets){
        globalSRSSets = new ArrayList<>(sets);
    }


    public static SRSQueue getglobalQueue(){
        return new SRSQueue("global", globalQueue());
    }


/*
    private static PriorityQueue<SRSEntry> globalQueue(){
        PriorityQueue<SRSEntry> gq = new PriorityQueue<>(20, new SRSEntryComparator());
        Map<Character, SRSEntry> countedChars = new HashMap<>();
        for(CharacterStudySet s: globalSRSSets){
            for(SRSEntry e: s.getProgressTracker().srsQueue.srsQueue){
                SRSEntry existingEntry = countedChars.get(e.character);
                if(existingEntry == null || existingEntry.nextPractice.isBefore(e.nextPractice)){
                    gq.remove(existingEntry);
                    gq.add(e);
                    countedChars.put(e.character, e);
                }
            }
        }
        return gq;
    }
 */
    private static PriorityQueue<SRSEntry> globalQueue(){
        PriorityQueue<SRSEntry> gq = new PriorityQueue<>(20, new SRSEntryComparator());
        Set<Character> countedChars = new HashSet<>();
        for(CharacterStudySet s: globalSRSSets){
            for(SRSEntry e: s.getProgressTracker().srsQueue.srsQueue){
                if(!countedChars.contains(e.character)) {
                    gq.add(e);
                    countedChars.add(e.character);
                }
            }
        }
        return gq;
    }

    public SRSQueue(String id) {
        this.id = id;
        srsQueue = new PriorityQueue<>(20, new SRSEntryComparator());
    }

    SRSQueue(String id, PriorityQueue<SRSEntry> queue) {
        this.id = id;
        this.srsQueue = queue;
    }

    public SRSEntry checkForEntry(Set<Character> notAvailSet, LocalDate now) {
        PriorityQueue<SRSEntry> queueToUse = useSRSGlobal ? globalQueue() : this.srsQueue;

        SRSEntry c = queueToUse.peek();

        // empty queue, just return
        if(c == null){ return null; }

        // great! Here's an SRS review.
        if(checkSrsIsReady(c, now, notAvailSet)){
            return c;
        }

        // if the earliest avail srs char is after today, then optimize and return
        if(c.nextPractice.isAfter(now)){
            return null;
        }

        // uncommon case: the next SRS character has been studied in the last n characters.
        // Need to look through the SRS queue in order until we hit the end, or one that is after today.
        // Current implementation is pretty slow, need to (shallow) clone the queue.
        PriorityQueue<SRSEntry> clone = new PriorityQueue<>(queueToUse);
        SRSEntry s;
        while((s = clone.poll()) != null){
            if(checkSrsIsReady(s, now, notAvailSet)){
                return s;
            }

            // don't go through the whole queue unless we have to
            if(s.nextPractice.isAfter(now)) {
                return null;
            }
        }

        return null;
    }

    static private boolean checkSrsIsReady(SRSEntry c, LocalDate now, Set<Character> notAvailSet){
        return !notAvailSet.contains(c.character) && (c.nextPractice.isBefore(now) || c.nextPractice.isEqual(now));
    }

    public int size() {
        return srsQueue.size();
    }

    public void clear() {
        this.srsQueue.clear();
    }

    public static class SRSEntry {
        public final Character character;
        public final String setId;
        public final LocalDate nextPractice;

        private SRSEntry(Character character, String setId, LocalDate nextPractice) {
            if(character == null || setId == null || nextPractice == null) {
                throw new IllegalArgumentException("null character passed to SRSEntry: character="
                        + character + "; setId=" + setId + "; nextPractice=" + nextPractice);
            }
            this.character = character;
            this.setId = setId;
            this.nextPractice = nextPractice;
        }

        public String toString(){
            return "[" + character + " " + nextPractice + " ]";
        }
    }

    private LocalDateTime lastLocalDateTime = null;
    private Character lastCharacter = null;

    public Iterator<SRSEntry> iterator(){
        if(useSRSGlobal){
            return globalQueue().iterator();
        }
        return srsQueue.iterator();
    }

    public SRSEntry addToSRSQueue(Character character, int score, LocalDateTime timestamp, int knownScoreValue){
        if(timestamp.equals(lastLocalDateTime) && character.equals(lastCharacter)){
            return null;
        }

        lastLocalDateTime = timestamp;
        lastCharacter = character;

        if(score < 0 || score == knownScoreValue){
            removeSRSQueue(character);
            return null;
        }

        // remove any existing entries
        removeSRSQueue(character);

        // schedule next
        Period delay = SRSTable[score];
        LocalDate nextDate = timestamp.plus(delay).toLocalDate();
        SRSEntry entry = new SRSEntry(character, id, nextDate);

        srsQueue.add(entry);

        return entry;
    }


    public SRSEntry find(Character c){
        Iterator<SRSEntry> it = srsQueue.iterator();
        while(it.hasNext()){
            SRSEntry e = it.next();
            if(e.character.equals(c)){
                return e;
            }
        }
        return null;
    }

    public boolean find(Character c, LocalDate forTime){
        Iterator<SRSEntry> it = srsQueue.iterator();
        while(it.hasNext()){
            SRSEntry e = it.next();
            if(e.character.equals(c) && (e.nextPractice.isBefore(forTime) || e.nextPractice.isEqual(forTime))){
                return true;
            }
        }
        return false;
    }

    public boolean removeSRSQueue(Character c){
        for (SRSEntry e : srsQueue) {
            if (e.character.equals(c)) {
                return srsQueue.remove(e);
            }
        }
        return false;
    }

    private static class SRSEntryComparator implements Comparator<SRSEntry> {
        @Override
        public int compare(SRSEntry o1, SRSEntry o2) {
            int compareDate = o1.nextPractice.compareTo(o2.nextPractice);
            if(compareDate != 0){
                return compareDate;
            }

            // if scheduled dates are equal, ensure a consistent ordering
            return o1.character.compareTo(o2.character);
        }

        @Override
        public boolean equals(Object obj) {
            return obj != null && obj.getClass().equals(this.getClass());
        }
    }

    public Map<LocalDate, List<Character>> getSrsSchedule() {
        PriorityQueue<SRSEntry> queueToUse = useSRSGlobal ? globalQueue() : this.srsQueue;

        Map<LocalDate, List<Character>> out = new LinkedHashMap<>();
        SRSQueue.SRSEntry[] entries = queueToUse.toArray(new SRSQueue.SRSEntry[0]);
        Arrays.sort(entries, new SRSEntryComparator());
        for(SRSEntry s: entries){
            List<Character> list = out.get(s.nextPractice);
            if(list == null){
                list = new ArrayList<>();
                out.put(s.nextPractice, list);
            }
            list.add(s.character);
        }
        return out;
    }

    public void debugAddDayToSRS() {
        PriorityQueue<SRSEntry> copy = new PriorityQueue<>(srsQueue);
        for(SRSEntry e: copy){
            srsQueue.remove(e);
            SRSEntry newE = new SRSEntry(e.character, id, e.nextPractice.minusDays(1));
            srsQueue.add(newE);
        }
    }

    public String getSrsScheduleString() {
        return id + ": " + Util.join(getSrsSchedule(), ": ", ", ");
    }

    public String serializeOut() throws  IOException {
        StringWriter sw = new StringWriter();
        JsonWriter jw = new JsonWriter(sw);
        serializeOut(jw);
        jw.close();
        return sw.toString();
    }

    public void serializeOut(JsonWriter j) throws  IOException{
        SRSEntry[] entries = srsQueue.toArray(new SRSEntry[0]);
        j.beginArray();
        for(SRSEntry s: entries){
            j.beginObject();
            j.name("character");
            j.value(s.character.toString());
            j.name("nextPractice");
            j.value(s.nextPractice.toString());
            j.endObject();
        }
        j.endArray();
    }

    public static SRSQueue deserializeIn(String id, String queueJSON) throws IOException{
        PriorityQueue<SRSEntry> queue = new PriorityQueue<>(20, new SRSEntryComparator());
        JsonReader jr = new JsonReader(new StringReader(queueJSON));
        jr.beginArray();
        while(jr.hasNext()){
            jr.beginObject();
            while(jr.hasNext()){
                "character".equals(jr.nextName());
                String character = jr.nextString();
                "nextPractice".equals(jr.nextName());
                String date = jr.nextString();
                queue.add(new SRSEntry(character.charAt(0), id, LocalDate.parse(date)));
            }
            jr.endObject();
        }
        jr.endArray();
        jr.close();
        return new SRSQueue(id, queue);
    }

    // There was a bug where a set could save entries in its serialized SRSQueue for characters
    // not in its set. These would get stuck looping forever. Detect and clear them out here.
    public void correctSRSQueueState(Set<Character> allCharactersSet) {
        List<SRSEntry> toRemove = new ArrayList<>();
        for(SRSEntry s: srsQueue){
            if(!allCharactersSet.contains(s.character)){
                toRemove.add(s);
            }
        }

        int removed = 0;
        for(SRSEntry s: toRemove){
            srsQueue.remove(s);
            removed++;
        }
    }

}
