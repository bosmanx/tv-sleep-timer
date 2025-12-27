package pl.haxen.tvsleep;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

public class SleepAccessibilityService extends AccessibilityService {
    private static final String TAG = "TVSleep";
    private static final int KEYCODE_PROG_RED = 183;
    
    private static final int[] TIMER_OPTIONS = {3600, 1800, 900, 5, 0}; 
    private static final String[] LABELS = {"60 min", "30 min", "15 min", "5 sec", "OFF"};
    private int currentTimerIndex = 4; // Start at OFF
    private int remainingSeconds = 0;
    
    private Handler handler;
    private Runnable countdownRunnable;
    private ScreenOnReceiver screenOnReceiver;
    private long lastTogglePressTime = 0;
    private static final long TOGGLE_WINDOW_MS = 3000; // 3 seconds to toggle
    
    private static final int DEFAULT_TIMEOUT = 600000; // 10 minutes

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {
        stopCountdown();
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.i(TAG, "Service Connected");
        handler = new Handler(Looper.getMainLooper());
        
        // Register SCREEN_ON listener
        try {
            screenOnReceiver = new ScreenOnReceiver();
            registerReceiver(screenOnReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
        } catch (Exception ignored) {}
        
        showToast("Sleep Timer Ready");
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KEYCODE_PROG_RED && event.getAction() == KeyEvent.ACTION_DOWN) {
            Log.i(TAG, "Red button pressed");
            handleRedButton();
            return true;
        }
        return super.onKeyEvent(event);
    }

    private void handleRedButton() {
        long now = System.currentTimeMillis();
        boolean withinWindow = (now - lastTogglePressTime) < TOGGLE_WINDOW_MS;
        
        // If timer is ON and this is the FIRST press (outside 3s window)
        if (remainingSeconds > 0 && !withinWindow) {
            int mins = remainingSeconds / 60;
            int secs = remainingSeconds % 60;
            String msg = "Remaining: " + (mins > 0 ? mins + "m " : "") + secs + "s";
            showToast(msg);
            lastTogglePressTime = now;
            return;
        }

        // Toggling logic (either it's the second press or timer was OFF)
        lastTogglePressTime = now;
        stopCountdown(); // Use stopCountdown instead of cancelTimer to avoid double reset
        currentTimerIndex = (currentTimerIndex + 1) % TIMER_OPTIONS.length;
        remainingSeconds = TIMER_OPTIONS[currentTimerIndex];

        if (remainingSeconds > 0) {
            startCountdown();
            showToast("Sleep timer: " + LABELS[currentTimerIndex]);
        } else {
            showToast("Sleep timer: OFF");
        }
    }

    private void startCountdown() {
        if (countdownRunnable != null) handler.removeCallbacks(countdownRunnable);
        
        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                if (remainingSeconds > 0) {
                    remainingSeconds--;
                    if (remainingSeconds == 0) {
                        executeSleep();
                    } else {
                        // Toast reminders at key points
                        if (remainingSeconds == 300 || remainingSeconds == 60 || remainingSeconds == 5) {
                            showToast("TV Sleeping in " + (remainingSeconds >= 60 ? (remainingSeconds/60) + "m" : remainingSeconds + "s"));
                        }
                        handler.postDelayed(this, 1000);
                    }
                }
            }
        };
        handler.postDelayed(countdownRunnable, 1000);
    }

    private void stopCountdown() {
        if (countdownRunnable != null) {
            handler.removeCallbacks(countdownRunnable);
            countdownRunnable = null;
        }
        remainingSeconds = 0;
    }

    private void executeSleep() {
        try {
            if (Settings.System.canWrite(this)) {
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 5000);
                Log.i(TAG, "Sleep triggered");
            }
        } catch (Exception ignored) {}
    }

    private void restoreTimeout() {
        try {
            if (Settings.System.canWrite(this)) {
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, DEFAULT_TIMEOUT);
                Log.i(TAG, "Timeout restored");
            }
        } catch (Exception ignored) {}
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        try { unregisterReceiver(screenOnReceiver); } catch (Exception ignored) {}
        stopCountdown();
        super.onDestroy();
    }

    private class ScreenOnReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                restoreTimeout();
            }
        }
    }
}
