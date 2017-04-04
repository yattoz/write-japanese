package dmeeuwis.nakama;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.UUID;

import dmeeuwis.kanjimaster.BuildConfig;
import dmeeuwis.kanjimaster.R;
import dmeeuwis.nakama.data.CharacterSets;
import dmeeuwis.nakama.data.CharacterStudySet;
import dmeeuwis.nakama.data.UncaughtExceptionLogger;
import dmeeuwis.nakama.primary.Iid;
import dmeeuwis.nakama.primary.KanjiMasterActivity;

public class ReminderManager extends BroadcastReceiver {

    private static final String INTENT_CHARSET = "charset";
    private static final boolean DEBUG_REMINDERS = false;

    private static Intent makeIntent(Context c, CharacterStudySet charset){
        Intent intent = new Intent(c, ReminderManager.class);
        intent.putExtra("charset", charset.pathPrefix);
        return intent;
    }

    private static int makePendingId(CharacterStudySet charset) {
        return charset.pathPrefix.hashCode();
    }

    public static void scheduleRemindersFor(Context c, CharacterStudySet charset) {
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

        Log.i("nakama", "Setting study reminder for charset " + charset + ": " + df.format(calendar.getTime()) + " " + makePendingId(charset));
        PendingIntent pendingIntent = PendingIntent.getBroadcast(c, makePendingId(charset),
                    makeIntent(c, charset), PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        Log.i("nakama", "ReminderManager: scheduled a notification for " + calendar.getTimeInMillis() + "!");
    }

    public static void clearAllReminders(Activity c){
        final String[] set = { "j1", "j2", "j3", "j4", "j5", "j6", "hiragana", "katakana" };
        for(String s: set){
            CharacterStudySet charset = CharacterSets.fromName(c, s, null);
            charset.load();
            clearReminders(c.getApplicationContext(), charset);
        }
        Log.i("nakama", "ReminderManager: cleared all notification!");
    }

    public static boolean reminderExists(Context c, CharacterStudySet charset){
        PendingIntent pendingIntent = PendingIntent.getBroadcast(c, makePendingId(charset),
                makeIntent(c, charset), PendingIntent.FLAG_NO_CREATE);
        Log.i("nakama-remind", "Checking reminder for charset + " + charset + ": " + makePendingId(charset) + " "  + (pendingIntent != null));
        return pendingIntent != null;
    }

    public static void clearReminders(Context c, CharacterStudySet charset){
        AlarmManager alarmManager = (AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(c, makePendingId(charset), makeIntent(c, charset), PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
       Log.i("nakama", "ReminderManager: cleared notification for " + charset.pathPrefix + " " + makePendingId(charset));
    }

    public void onReceive(Context context, Intent intent) {
        try {
            Log.i("nakama", "ReminderManager.onReceive: wakeup!");
            String charset = intent.getStringExtra(INTENT_CHARSET);

            UUID iid = Iid.get(context.getApplicationContext());
            Log.i("nakama-remind", "Found iid in reminder notification task as: " + iid);
            CharacterStudySet set = CharacterSets.fromName(context, charset, null);
            set.load();
            CharacterStudySet.GoalProgress gp = set.getGoalProgress();
            if (set == null) {
                return;
            }

            int charCount = gp.neededPerDay;

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            int id = set.pathPrefix.hashCode();

            // clear any existing notification that user didn't click
            notificationManager.cancel(id);

            // add new notification
            Notification n = getNotification(context, "Study Reminder", "Try for " + charCount + " " + set.name + " characters today!");
            notificationManager.notify(id, n);

            // schedule tomorrow's reminder
            Calendar now = Calendar.getInstance();
            now.setTimeInMillis(System.currentTimeMillis());
            if (gp.goal.after(now)) {
                scheduleRemindersFor(context, set);
            }
        } catch(Throwable t){
            UncaughtExceptionLogger.backgroundLogError("Caught error in reminder service onReceive", t, context);
        }
    }

    private Notification getNotification(Context context, String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setContentTitle(title);
        builder.setContentText(message);
        builder.setAutoCancel(true);
        builder.setSmallIcon(R.drawable.ic_language_white);
        Intent intent = new Intent(context, KanjiMasterActivity.class);
        PendingIntent pi = PendingIntent.getActivity(context, PendingIntent.FLAG_ONE_SHOT, intent, 0);
        builder.setContentIntent(pi);
        return builder.build();
    }
}