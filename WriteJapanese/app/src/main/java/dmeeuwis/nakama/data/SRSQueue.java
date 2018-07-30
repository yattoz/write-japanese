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

import dmeeuwis.kanjimaster.BuildConfig;
import dmeeuwis.util.Util;

public class SRSQueue {
    public static final SRSQueue GLOBAL = new SRSQueue("global");

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

    public SRSEntry peek() {
        return srsQueue.peek();
    }

    public SRSEntry poll() {
        return srsQueue.peek();
    }

    public int size() {
        return srsQueue.size();
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
        return srsQueue.iterator();
    }

    public SRSEntry addToSRSQueue(Character character, int score, LocalDateTime timestamp, int knownScoreValue){
        if(timestamp == lastLocalDateTime && character.equals(lastCharacter)){
            Log.d("nakama-progress", "Skipping dup as seen by queue: " + character + " " + timestamp);
            return null;
        }
        lastLocalDateTime = timestamp;
        lastCharacter = character;

        //Log.d("nakama-progress", "------------------------ addToSrsQueue -----------------------");
        if(score < 0 || score == knownScoreValue){
            if(BuildConfig.DEBUG){
                //Log.d("nakama-progress", "Removing " + character + " from SRS due to score " + score);
                //Log.d("nakama-progress", "Set is now: " + getSrsScheduleString());
            }
            removeSRSQueue(character);
            return null;
        }
        //Log.d("nakama-progress", "Prior to adding, set is: " + getSrsScheduleString());

        // remove any existing entries
        removeSRSQueue(character);

        // schedule next
        Period delay = SRSTable[score];
        if(BuildConfig.DEBUG){
            //Log.d("nakama-progress", "Setting delay to " + delay + " for score " + score + " on char " + character);
        }
        LocalDate nextDate = timestamp.plus(delay).toLocalDate();
        SRSEntry entry = new SRSEntry(character, nextDate);
        //if(BuildConfig.DEBUG) Log.d("nakama-progress", "Adding entry: " + entry + " to set " + this);

        srsQueue.add(entry);
        //if(BuildConfig.DEBUG) Log.d("nakama-progress", "After adding, set is: " + getSrsScheduleString());

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
        Iterator<SRSEntry> it = srsQueue.iterator();
        while(it.hasNext()){
            SRSEntry e = it.next();
            if(e.character.equals(c)){

                //Log.d("nakama-progress", "------------------------ removeSRSQueue -----------------------");
                //Log.d("nakama-progress", "Removing char " + c + "; set before was " + getSrsScheduleString());

                srsQueue.remove(e);

                //Log.d("nakama-progress", "Removing char " + c + "; set after was " + getSrsScheduleString());

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
        Map<LocalDate, List<Character>> out = new LinkedHashMap<>();
        SRSQueue.SRSEntry[] entries = srsQueue.toArray(new SRSQueue.SRSEntry[0]);
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
        PriorityQueue<SRSEntry> copy = new PriorityQueue<>(this.srsQueue);
        for(SRSEntry e: copy){
            srsQueue.remove(e);
            SRSEntry newE = new SRSEntry(e.character, e.nextPractice.minusDays(1));
            srsQueue.add(newE);
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

    public SRSQueue deserializeIn(String id, String queueJSON) throws IOException{
        PriorityQueue<SRSEntry> queue = new PriorityQueue<>(new SRSEntryComparator());
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
