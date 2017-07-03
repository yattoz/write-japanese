package dmeeuwis.nakama.data;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Html;

import org.threeten.bp.LocalDate;

import java.util.List;
import java.util.Map;

public class SRSScheduleHtmlGenerator {

    public final static String MESSAGE = "Write Japanese's Spaced Repitition System (SRS) repeats correctly " +
            "drawn characters after a scheduled time delay. With each correct resposne, the delay time is increased. " +
            "Based on your previous practice sessions, here is the current SRS schedule: ";

    public static void displayScheduleDialog(Context ctx, Map<LocalDate, List<Character>> schedule){
        AlertDialog.Builder b = new AlertDialog.Builder(ctx);
        b.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //do things
                    }
                });

        AlertDialog d = b.create();
        String html = generateHtml(schedule);
        d.setMessage(Html.fromHtml(html));
        d.show();
    }


    public static String generateHtml(Map<LocalDate, List<Character>> schedule){
        StringBuilder sb = new StringBuilder();

        sb.append("<h1>Spaced Repitiion Schedule</h1>");
        sb.append("<b>" + MESSAGE + "</b>");

        for(Map.Entry<LocalDate, List<Character>> e: schedule.entrySet()){
            sb.append("<h3>");
            sb.append(e.getKey());
            sb.append("</h3>");

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
