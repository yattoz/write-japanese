package dmeeuwis.nakama.data;

import android.support.annotation.NonNull;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.Period;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import dmeeuwis.kanjimaster.BuildConfig;
import dmeeuwis.util.Util;

public class SRSQueue {
    public static SRSQueue GLOBAL = new SRSQueue("globalSRS");

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

    public SRSQueue(String id) {
        this.id = id;
        srsQueue = new PriorityQueue<>(20, new SRSEntryComparator());
    }

    SRSQueue(String id, PriorityQueue<SRSEntry> queue) {
        this.id = id;
        this.srsQueue = queue;
    }

    public SRSEntry checkForEntry(Set<Character> notAvailSet, LocalDate now) {
        PriorityQueue<SRSEntry> queueToUse = useSRSGlobal ? GLOBAL.srsQueue : this.srsQueue;

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
        public final LocalDate nextPractice;

        private SRSEntry(Character character, LocalDate nextPractice) {
            this.character = character;
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
            return GLOBAL.srsQueue.iterator();
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
        SRSEntry entry = new SRSEntry(character, nextDate);

        srsQueue.add(entry);
        GLOBAL.srsQueue.add(entry);

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

    public void removeSRSQueue(Character c){
        {
            for (SRSEntry e : srsQueue) {
                if (e.character.equals(c)) {
                    srsQueue.remove(e);
                    break;
                }
            }
        }

        for (SRSEntry e : GLOBAL.srsQueue) {
            if (e.character.equals(c)) {
                GLOBAL.srsQueue.remove(e);
                break;
            }
        }
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
        PriorityQueue<SRSEntry> queueToUse = useSRSGlobal ? GLOBAL.srsQueue : this.srsQueue;

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
        PriorityQueue<SRSEntry> queueToUse = useSRSGlobal ? GLOBAL.srsQueue : this.srsQueue;
        PriorityQueue<SRSEntry> copy = new PriorityQueue<>(queueToUse);
        for(SRSEntry e: copy){
            queueToUse.remove(e);
            SRSEntry newE = new SRSEntry(e.character, e.nextPractice.minusDays(1));
            queueToUse.add(newE);
        }
    }

    public String getSrsScheduleString() {
        return id + ": " + Util.join(getSrsSchedule(), ": ", ", ");
    }

    public String serializeOut() throws  IOException{
        StringWriter sw = new StringWriter();
        JsonWriter j = new JsonWriter(sw);
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
        j.close();
        return sw.toString();
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
                queue.add(new SRSEntry(character.charAt(0), LocalDate.parse(date)));
            }
            jr.endObject();
        }
        jr.endArray();
        jr.close();
        return new SRSQueue(id, queue);
    }
}
