package pl.haxen.tvsleep;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final String TAG = "TVSleep";
    private Button setupButton;
    private Button writeSettingsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.i(TAG, "========================================");
        Log.i(TAG, "TV Sleep Timer - Starting");
        Log.i(TAG, "========================================");

        // Initialize setup buttons
        setupButton = findViewById(R.id.btnSetup);
        setupButton.setOnClickListener(v -> openAccessibilitySettings());
        
        writeSettingsButton = findViewById(R.id.btnWriteSettings);
        writeSettingsButton.setOnClickListener(v -> openWriteSettingsPermission());

        // Display device information
        displayDeviceInfo();

        // Check accessibility service status
        checkAccessibilityServiceStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-check when user returns from settings
        updateServiceStatus();
    }

    private void displayDeviceInfo() {
        TextView deviceInfoText = findViewById(R.id.deviceInfo);
        
        StringBuilder info = new StringBuilder();
        info.append("📺 DEVICE INFORMATION\n\n");
        
        // Basic device info
        info.append("Manufacturer: ").append(Build.MANUFACTURER).append("\n");
        info.append("Model: ").append(Build.MODEL).append("\n");
        info.append("Android: ").append(Build.VERSION.RELEASE).append(" (SDK ").append(Build.VERSION.SDK_INT).append(")\n\n");
        
        // Sharp TV specific notes
        if (Build.MANUFACTURER.equalsIgnoreCase("Sharp")) {
            info.append("⚠️ SHARP TV NOTES:\n");
            info.append("• If permission UI fails, use ADB:\n");
            info.append("  adb shell appops set pl.haxen.tvsleep WRITE_SETTINGS allow\n");
            info.append("• SCREEN_ON restores timeout on wake\n\n");
        }
        
        // How it works
        info.append("⚙️ HOW IT WORKS:\n");
        info.append("• Detects RED button press\n");
        info.append("• Sets timeout to 5 seconds\n");
        info.append("• SCREEN_ON restores timeout on wake\n\n");
        
        // Timer options
        info.append("⏱️ TIMER OPTIONS:\n");
        info.append("Press RED button to cycle:\n");
        info.append("60 min → 30 min → 15 min → 5 sec → OFF\n\n");
        
        // Warnings at countdown
        info.append("🔔 COUNTDOWN ALERTS:\n");
        info.append("Toast at 5 min, 1 min, 5 sec\n");
        
        deviceInfoText.setText(info.toString());
    }

    private void checkAccessibilityServiceStatus() {
        boolean isEnabled = isAccessibilityServiceEnabled();
        Log.i(TAG, "Accessibility Service enabled: " + isEnabled);
        
        updateServiceStatus();
    }

    private void updateServiceStatus() {
        TextView statusText = findViewById(R.id.statusText);
        
        boolean accessibilityEnabled = isAccessibilityServiceEnabled();
        boolean writeSettingsGranted = Settings.System.canWrite(this);
        
        Log.i(TAG, "Accessibility Service: " + accessibilityEnabled);
        Log.i(TAG, "WRITE_SETTINGS permission: " + writeSettingsGranted);
        
        // Both permissions required
        if (accessibilityEnabled && writeSettingsGranted) {
            statusText.setText("✅ SLEEP TIMER ACTIVE\n\nPress RED button on your remote to set timer");
            setupButton.setVisibility(View.GONE);
            writeSettingsButton.setVisibility(View.GONE);
            Log.i(TAG, "All permissions granted - Ready!");
        } else {
            // Show which permissions are missing
            StringBuilder message = new StringBuilder("⚠️ SETUP REQUIRED\n\n");
            if (!accessibilityEnabled) {
                message.append("• Enable Accessibility Service\n");
            }
            if (!writeSettingsGranted) {
                message.append("• Grant WRITE_SETTINGS permission");
            }
            statusText.setText(message.toString());
            
            // Show appropriate buttons
            if (!accessibilityEnabled) {
                setupButton.setVisibility(View.VISIBLE);
                setupButton.requestFocus();
            } else {
                setupButton.setVisibility(View.GONE);
            }
            
            if (!writeSettingsGranted) {
                writeSettingsButton.setVisibility(View.VISIBLE);
                if (accessibilityEnabled) {
                    writeSettingsButton.requestFocus();
                }
            } else {
                writeSettingsButton.setVisibility(View.GONE);
            }
            
            Log.i(TAG, "Missing permissions - Buttons shown");
        }
    }

    private boolean isAccessibilityServiceEnabled() {
        String serviceName = getPackageName() + "/" + SleepAccessibilityService.class.getCanonicalName();
        String enabledServices = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        
        if (enabledServices == null) {
            return false;
        }
        
        return enabledServices.contains(serviceName);
    }

    private void openAccessibilitySettings() {
        showDisclosureDialog();
    }

    private void showDisclosureDialog() {
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(R.string.disclosure_title)
            .setMessage(R.string.disclosure_message)
            .setPositiveButton(R.string.disclosure_agree, (dialog, which) -> {
                proceedToAccessibilitySettings();
            })
            .setNegativeButton(R.string.disclosure_exit, (dialog, which) -> {
                finish();
            })
            .setCancelable(false)
            .show();
    }

    private void proceedToAccessibilitySettings() {
        Log.i(TAG, "Opening Accessibility Settings (Leanback Method)...");
        Toast.makeText(this, "Opening Settings...", Toast.LENGTH_SHORT).show();
        
        try {
            // This is the most compatible "standard" way for Leanback TVs
            Intent intent = new Intent("com.android.tv.settings.action.ACCESSIBILITY_SETTINGS");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            Log.i(TAG, "SUCCESS: Opened via action string");
        } catch (Exception e) {
            Log.w(TAG, "Action string failed, trying Fragment Method: " + e.getMessage());
            
            try {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("com.android.tv.settings", "com.android.tv.settings.MainSettings"));
                intent.putExtra("fragmentClassName", "com.android.tv.settings.accessibility.AccessibilityFragment");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception e2) {
                // Final fallback
                try {
                    startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    Toast.makeText(this, "Select 'Device Preferences' -> 'Accessibility'", Toast.LENGTH_LONG).show();
                } catch (Exception e3) {
                    Log.e(TAG, "All attempts failed");
                }
            }
        }
    }
    
    private void openWriteSettingsPermission() {
        try {
            Log.i(TAG, "Opening WRITE_SETTINGS permission screen...");
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(android.net.Uri.parse("package:" + getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            Log.i(TAG, "SUCCESS: Started WRITE_SETTINGS permission screen");
        } catch (Exception e) {
            Log.e(TAG, "========================================");
            Log.e(TAG, "FAILED to open WRITE_SETTINGS permission!");
            Log.e(TAG, "Error: " + e.getMessage());
            Log.e(TAG, "========================================");
            Log.w(TAG, "Sharp TV may not support this - grant via ADB:");
            Log.w(TAG, "adb shell appops set " + getPackageName() + " WRITE_SETTINGS allow");
            e.printStackTrace();
        }
    }
}
