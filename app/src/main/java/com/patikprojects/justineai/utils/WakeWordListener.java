package com.patikprojects.justineai.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.patikprojects.justineai.activity.SpeechHomeActivity;
import com.patikprojects.justineai.assist.JustineAssistantActivity;

import java.util.List;
import java.util.Objects;

import ai.picovoice.porcupine.Porcupine;
import ai.picovoice.porcupine.PorcupineException;
import ai.picovoice.porcupine.PorcupineManager;

public class WakeWordListener {
    private static final String TAG = "WakeWordListener";
    private static PorcupineManager porcupineManager;
    private static boolean isRunning = false;

    public static void start(Context context) {
        if (isRunning) {
            Log.i(TAG, "Wake word listener already running");
            return;
        }

        Log.i(TAG, "Attempting to start Porcupine...");
        try {
            porcupineManager = new PorcupineManager.Builder()
                    .setAccessKey("PNfRVdp1FCBn4CABSlXY8e4LnyAZbhpp1ab7zZcWv6JdBa2OTMkLFw==")
                    .setKeywordPath("keywords/hey-justine.ppn")
                    .setSensitivity(0.87f)
                    .build(context, keywordIndex -> {
                        Log.i(TAG, "Hotword Detected: Hey Justine");
                        handleWakeWordDetection(context);
                    });

            porcupineManager.start();
            isRunning = true;
            Log.i(TAG, "Porcupine started successfully.");

        } catch (PorcupineException e) {
            Log.e(TAG, "Porcupine init error", e);
            isRunning = false;
        }
    }

    public static void stop() {
        if (porcupineManager != null) {
            try {
                porcupineManager.stop();
                porcupineManager.delete();
                porcupineManager = null;
                isRunning = false;
                Log.i(TAG, "Porcupine stopped successfully.");
            } catch (PorcupineException e) {
                Log.e(TAG, "Error stopping Porcupine", e);
            }
        }
    }

    public static boolean isRunning() {
        return isRunning;
    }

    private static void handleWakeWordDetection(Context context) {
        String currentActivity = getCurrentActivity(context);
        boolean appInForeground = isAppInForeground(context);

        if (appInForeground) {
            Log.i(TAG, "App in foreground, Current activity: " + currentActivity);
            if (currentActivity != null && currentActivity.contains("SpeechHomeActivity")) {
                Log.i(TAG, "Triggering speech recognition in SpeechHomeActivity");
                Intent intent = new Intent("START_RECOGNITION");
                context.sendBroadcast(intent);
            } else {
                Log.i(TAG, "App in foreground but not in SpeechHomeActivity, ignoring wake word");
            }
        } else {
            Log.i(TAG, "App in background, triggering screen");

            Intent floatingIntent = new Intent(context, SpeechHomeActivity.class);
            floatingIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TOP |
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
            );
            floatingIntent.putExtra("triggered_by_wake_word", true);
            context.startActivity(floatingIntent);
        }
    }

    private static boolean isAppInForeground(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        String packageName = context.getPackageName();

        if (appProcesses != null) {
            for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
                if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                        appProcess.processName.equals(packageName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String getCurrentActivity(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> taskInfo = activityManager.getRunningTasks(1);

        if (taskInfo != null && !taskInfo.isEmpty()) {
            return Objects.requireNonNull(taskInfo.get(0).topActivity).getClassName();
        }
        return null;
    }
}
