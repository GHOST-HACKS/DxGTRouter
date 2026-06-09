package com.dxgt.sniper.audiorouter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private Button toggleButton;
    private TextView statusText;
    private Button followButton;
    private ImageView rotatingRing;
    private ImageView revolvingGlacier;
    private View glowOverlay;
    private FrameLayout snowContainer;
    private boolean isActive = false;
    private SharedPreferences prefs;
    private static final int REQUEST_CODE_SCREEN_CAPTURE = 1;
    private static final int REQUEST_CODE_MICROPHONE = 2;
    private Handler snowHandler = new Handler();
    private RotateAnimation rotateRingAnim;
    private RotateAnimation rotateGlacierAnim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getResources().getIdentifier("activity_main", "layout", getPackageName()));

        toggleButton = (Button) findViewById(getResources().getIdentifier("toggleButton", "id", getPackageName()));
        statusText = (TextView) findViewById(getResources().getIdentifier("statusText", "id", getPackageName()));
        followButton = (Button) findViewById(getResources().getIdentifier("followButton", "id", getPackageName()));
        rotatingRing = (ImageView) findViewById(getResources().getIdentifier("rotatingRing", "id", getPackageName()));
        revolvingGlacier = (ImageView) findViewById(getResources().getIdentifier("revolvingGlacier", "id", getPackageName()));
        glowOverlay = findViewById(getResources().getIdentifier("glowOverlay", "id", getPackageName()));
        snowContainer = (FrameLayout) findViewById(getResources().getIdentifier("snowContainer", "id", getPackageName()));
        
        prefs = getSharedPreferences("DxGTStats", MODE_PRIVATE);

        // Request microphone permission
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, REQUEST_CODE_MICROPHONE);
        }

        // Show warning once
        if (!prefs.getBoolean("warningShown", false)) {
            showWarning();
        }

        // Request battery exemption
        requestBatteryExemption();

        // Start animations
        startAnimations();

        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isActive) {
                    if (!isAccessibilityServiceEnabled()) {
                        showAccessibilityDialog();
                        return;
                    }
                    startAudioRouting();
                } else {
                    stopAudioRouting();
                }
            }
        });

        followButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.tiktok.com/@dxgtsniper"));
                startActivity(intent);
                Toast.makeText(MainActivity.this, "Thanks for following! ❄️", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startAnimations() {
        rotateRingAnim = new RotateAnimation(0, 360,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f);
        rotateRingAnim.setDuration(8000);
        rotateRingAnim.setInterpolator(new LinearInterpolator());
        rotateRingAnim.setRepeatCount(Animation.INFINITE);
        
        rotateGlacierAnim = new RotateAnimation(0, -360,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f);
        rotateGlacierAnim.setDuration(12000);
        rotateGlacierAnim.setInterpolator(new LinearInterpolator());
        rotateGlacierAnim.setRepeatCount(Animation.INFINITE);
        
        rotatingRing.startAnimation(rotateRingAnim);
        revolvingGlacier.startAnimation(rotateGlacierAnim);
    }

    private void showAccessibilityDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("❄️ Accessibility Service Required");
        builder.setMessage("Please enable Accessibility Service in Settings → Accessibility → DxGT Glacier Router");
        builder.setPositiveButton("Open Settings", new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface dialog, int which) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showWarning() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("❄️ Important Notice ❄️");
        builder.setMessage("Android may close this app after 20-45 minutes to save battery.\n\nJust reopen and tap START again.");
        builder.setPositiveButton("I Understand", new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface dialog, int which) {
                prefs.edit().putBoolean("warningShown", true).apply();
            }
        });
        builder.show();
    }

    private void requestBatteryExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            try {
                startActivity(intent);
            } catch (Exception e) {}
        }
    }

    private boolean isAccessibilityServiceEnabled() {
        String service = getPackageName() + "/" + AudioRoutingService.class.getCanonicalName();
        try {
            String enabledServices = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            return enabledServices != null && enabledServices.contains(service);
        } catch (Exception e) {
            return false;
        }
    }

    private void startAudioRouting() {
        MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_CODE_SCREEN_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE && resultCode == RESULT_OK) {
            Intent serviceIntent = new Intent(this, ForegroundMicService.class);
            serviceIntent.putExtra("resultCode", resultCode);
            serviceIntent.putExtra("data", data);
            startService(serviceIntent);
            
            isActive = true;
            toggleButton.setText("ON 💙");
            toggleButton.setBackgroundColor(0xFF00BFFF);
            statusText.setText("⚡ STATUS: ACTIVE");
            if (glowOverlay != null) {
                glowOverlay.setVisibility(View.VISIBLE);
            }
            Toast.makeText(this, "✅ Audio routing active!", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopAudioRouting() {
        stopService(new Intent(this, ForegroundMicService.class));
        isActive = false;
        toggleButton.setText("OFF ❄️");
        toggleButton.setBackgroundColor(0xFF0D2B45);
        statusText.setText("⚡ STATUS: IDLE");
        if (glowOverlay != null) {
            glowOverlay.setVisibility(View.GONE);
        }
        Toast.makeText(this, "Audio routing stopped.", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isActive) {
            stopAudioRouting();
        }
    }
}
