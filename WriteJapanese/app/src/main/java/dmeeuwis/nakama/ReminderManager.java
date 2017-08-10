package dmeeuwis.nakama;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.util.Pair;
import android.util.Log;

import org.threeten.bp.LocalDate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import dmeeuwis.kanjimaster.BuildConfig;
import dmeeuwis.kanjimaster.R;
import dmeeuwis.nakama.data.CharacterSets;
import dmeeuwis.nakama.data.CharacterStudySet;
import dmeeuwis.nakama.data.CustomCharacterSetDataHelper;
import dmeeuwis.nakama.data.UncaughtExceptionLogger;
import dmeeuwis.nakama.primary.CharacterSetStatusFragment;
import dmeeuwis.nakama.primary.KanjiMasterActivity;

public class ReminderManager extends BroadcastReceiver {

    private static final boolean DEBUG_REMINDERS = false;
    private static final int NOTIFICATION_ID = 289343;

    private static Intent makeIntent(Context c){
        return new Intent(c, ReminderManager.class);
    }


    public static void scheduleRemindersFor(Context c) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Calendar calendar = GregorianCalendar.getInstance();
        Log.i("nakama", "Current time is: " + df.format(calendar.getTime()));

        if(BuildConfig.DEBUG && DEBUG_REMINDERS){
            calendar.add(Calendar.MINUTE, 1);
        } else {
            calendar.add(Calendar.HOUR, 24);
            calendar.set(Calendar.HOUR_OF_DAY, 6);
            calendar.set(Calendar.MINUTE, 0);
        }

        Log.i("nakama", "Setting study reminder : " + df.format(calendar.getTime()));
        PendingIntent pendingIntent = PendingIntent.getBroadcast(c, NOTIFICATION_ID,
                    makeIntent(c), PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        Log.i("nakama", "ReminderManager: scheduled a notification for " + calendar.getTimeInMillis() + "!");
    }

    public void onReceive(Context context, Intent intent) {
        try {
            Log.i("nakama", "ReminderManager.onReceive: wakeup!");
            StringBuilder notificationMessage = new StringBuilder();
            boolean srsNotices = false;
            boolean goalNotices = false;


            CustomCharacterSetDataHelper customHelper = new CustomCharacterSetDataHelper(context);
            List<CharacterStudySet> customSets = customHelper.getSets();
            List<CharacterStudySet> standardSets = Arrays.asList(CharacterSets.standardSets(null, context));

            List<CharacterStudySet> allSets = new ArrayList<>();
            allSets.addAll(customSets);
            allSets.addAll(standardSets);

            for(CharacterStudySet set: allSets){
                set.load(context);
            }

            // look for any srs hits
            int hits = 0;
            LocalDate now = LocalDate.now();
            for(CharacterStudySet set: allSets){
                Map<LocalDate, List<Character>> i = set.getSrsSchedule();
                for(LocalDate d: i.keySet()){
                    if(d.isBefore(now) || d.equals(now)){
                        hits += i.get(d).size();
                    }
                }
            }
            if(hits > 0){
                srsNotices = true;
                notificationMessage.append(hits + " timed review characters for today! ");
            }

            // iterate over character set study goals, see if any for today
            List<Pair<String, Integer>> setGoalCounts = new ArrayList<>();
            for(CharacterStudySet set: allSets){
                CharacterStudySet.GoalProgress gp = set.getGoalProgress();
                if(gp != null && gp.daysLeft != 0 && CharacterSetStatusFragment.checkIfReminderExists(context, set)){
                    int charCount = gp.neededPerDay;
                    setGoalCounts.add(Pair.create(set.name, charCount));
                }
            }

            if(setGoalCounts.size() == 1 ){
                Pair<String, Integer> g = setGoalCounts.get(0);
                goalNotices = true;
                notificationMessage.append("Intro " + g.second + " " + g.first + " character" + (g.second == 1 ? "" : "s") + " to meet your goal.");
            } else if (setGoalCounts.size() > 1){
                goalNotices = true;
                notificationMessage.append("Intro " );
                for(int i = 0; i < setGoalCounts.size(); i++){
                    Pair<String, Integer> g = setGoalCounts.get(i);
                    if(i != 0){ notificationMessage.append(", "); }
                    notificationMessage.append(g.second + " characters in " + g.first);
                }
                notificationMessage.append(" today to meet your intro goals.");
            }


            if(notificationMessage.length() > 0) {
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

                // clear any existing notification that user didn't click
                notificationManager.cancel(NOTIFICATION_ID);


                String title;
                if(srsNotices && !goalNotices){
                    title = "Timed Review Reminder";
                } else if (goalNotices && !srsNotices){
                    title = "Set Goal Reminder";
                } else {
                    title = "Timed Review and Goal Reminder";
                }

                // add new notification
                Notification n = getNotification(context, title, notificationMessage.toString());
                notificationManager.notify(NOTIFICATION_ID, n);
            }

            // schedule tomorrow's possible reminder
            scheduleRemindersFor(context);
        } catch(Throwable t){
            UncaughtExceptionLogger.backgroundLogError("Caught error in reminder service onReceive", t, context);
        }
    }

    private Notification getNotification(Context context, String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setContentTitle(title);
        builder.setContentText(message);
        builder.setAutoCancel(true);

        if(Build.VERSION.SDK_INT >= 21) {
            builder.setVisibility(Notification.VISIBILITY_PUBLIC);
        }

        builder.setSmallIcon(R.drawable.ic_language_white);
        Intent intent = new Intent(context, KanjiMasterActivity.class);
        PendingIntent pi = PendingIntent.getActivity(context, PendingIntent.FLAG_ONE_SHOT, intent, 0);
        builder.setContentIntent(pi);
        return builder.build();
    }
}