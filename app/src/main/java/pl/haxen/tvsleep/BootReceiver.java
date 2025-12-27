package pl.haxen.tvsleep;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Log;

/**
 * Restores screen timeout to normal value on device boot
 * This ensures TV doesn't stay in short-timeout mode after sleep/wake cycle
 */
public class BootReceiver extends BroadcastReceiver {
    
    private static final String TAG = "TVSleep_BootReceiver";
    private static final int DEFAULT_TIMEOUT = 600000; // 10 minutes
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.i(TAG, "========================================");
            Log.i(TAG, "BOOT_COMPLETED - Restoring screen timeout");
            Log.i(TAG, "========================================");
            
            restoreScreenTimeout(context);
        }
    }
    
    private void restoreScreenTimeout(Context context) {
        try {
            // Check if we have WRITE_SETTINGS permission
            if (Settings.System.canWrite(context)) {
                // Restore to default 10 minutes
                Settings.System.putInt(context.getContentResolver(),
                        Settings.System.SCREEN_OFF_TIMEOUT, DEFAULT_TIMEOUT);
                
                Log.i(TAG, "SUCCESS: Screen timeout restored to " + DEFAULT_TIMEOUT + "ms");
            } else {
                Log.w(TAG, "WRITE_SETTINGS permission not available");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to restore timeout: " + e.getMessage());
        }
    }
}

