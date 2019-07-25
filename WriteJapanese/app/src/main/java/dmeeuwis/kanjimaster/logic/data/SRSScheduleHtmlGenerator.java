package dmeeuwis.kanjimaster.logic.data;

import org.threeten.bp.LocalDate;

import java.util.List;
import java.util.Map;

public class SRSScheduleHtmlGenerator {

    public final static String MESSAGE = "SRS repeats correctly " +
            "drawn characters after a scheduled time delay. With each correct response, the delay time is increased. " +
            "From your previous practice, here is your customized review schedule: ";

    public static String generateHtml(Map<LocalDate, List<Character>> schedule){
        StringBuilder sb = new StringBuilder();

        if(schedule.size() == 0){
            sb.append("<p>No reviews currently scheduled - as you correctly draw characters, they will begin to appear here with increasing timed delays.</p>");
            return sb.toString();
        }

        sb.append("<h1>Spaced Repitition Schedule</h1>");
        sb.append("<p>" + MESSAGE + "</p>");

        for(Map.Entry<LocalDate, List<Character>> e: schedule.entrySet()){
            sb.append("<h4>");
            sb.append(e.getKey());
            sb.append("</h4>");

            sb.append("<p>");
            for(Character c: e.getValue()){
                sb.append(" ");
                sb.append(c);
            }
            sb.append("</p>");
        }

        return sb.toString();
    }
}
