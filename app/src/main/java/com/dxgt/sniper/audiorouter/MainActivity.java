package com.dxgt.sniper.audiorouter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
    private Handler snowHandler = new Handler(Looper.getMainLooper());
    private RotateAnimation rotateRingAnim;
    private RotateAnimation rotateGlacierAnim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        toggleButton = findViewById(R.id.toggleButton);
        statusText = findViewById(R.id.statusText);
        followButton = findViewById(R.id.followButton);
        rotatingRing = findViewById(R.id.rotatingRing);
        revolvingGlacier = findViewById(R.id.revolvingGlacier);
        glowOverlay = findViewById(R.id.glowOverlay);
        snowContainer = findViewById(R.id.snowContainer);
        
        prefs = getSharedPreferences("DxGTStats", MODE_PRIVATE);

        // Request microphone permission
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                new String[]{android.Manifest.permission.RECORD_AUDIO},
                REQUEST_CODE_MICROPHONE);
        }

        // Show warning once
        if (!prefs.getBoolean("warningShown", false)) {
            showWarning();
        }

        // Request battery exemption
        requestBatteryExemption();

        // Start animations
        startAnimations();

        // Toggle button logic
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

        // Follow button
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
        // Ring rotates clockwise
        rotateRingAnim = new RotateAnimation(0, 360,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f);
        rotateRingAnim.setDuration(8000);
        rotateRingAnim.setInterpolator(new LinearInterpolator());
        rotateRingAnim.setRepeatCount(Animation.INFINITE);
        
        // Glacier revolves counter-clockwise
        rotateGlacierAnim = new RotateAnimation(0, -360,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f);
        rotateGlacierAnim.setDuration(12000);
        rotateGlacierAnim.setInterpolator(new LinearInterpolator());
        rotateGlacierAnim.setRepeatCount(Animation.INFINITE);
        
        rotatingRing.startAnimation(rotateRingAnim);
        revolvingGlacier.startAnimation(rotateGlacierAnim);
    }

    private void startSnowfall() {
        snowHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isActive) {
                    createSnowflake();
                    snowHandler.postDelayed(this, 500);
                }
            }
        });
    }

    private void createSnowflake() {
        final View snowflake = new View(this);
        snowflake.setBackgroundColor(0xFFFFFFFF);
        
        int size = 10 + (int)(Math.random() * 15);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
        params.leftMargin = (int)(Math.random() * snowContainer.getWidth());
        params.topMargin = 0;
        snowflake.setLayoutParams(params);
        snowflake.setAlpha(0.7f);
        
        snowContainer.addView(snowflake);
        
        snowflake.animate()
            .translationY(snowContainer.getHeight())
            .rotation((float)(Math.random() * 360))
            .setDuration(3000 + (int)(Math.random() * 2000))
            .withEndAction(new Runnable() {
                @Override
                public void run() {
                    snowContainer.removeView(snowflake);
                }
            })
            .start();
    }

    private void stopSnowfall() {
        snowHandler.removeCallbacksAndMessages(null);
        snowContainer.removeAllViews();
    }

    private void showAccessibilityDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("❄️ Accessibility Service Required");
        builder.setMessage("For DxGT Glacier Router to share your microphone between PUBG and TikTok simultaneously, please enable the Accessibility Service.\n\nGo to: Settings → Accessibility → Downloaded Services → DxGT Glacier Router\n\nOn Huawei devices, you may need to first allow 'Restricted Settings' in App Info.");
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
        builder.setTitle("❄️ Important Notice for Streamers ❄️");
        builder.setMessage("Android may forcefully close this app after 20-45 minutes of use to save battery.\n\nTo continue streaming, simply re-open the app and tap START again.\n\n❄️ Snowfall activates when mic sharing is ON!\n\nThis is an Android system limitation, not a bug.");
        builder.setPositiveButton("I Understand", new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface dialog, int which) {
                prefs.edit().putBoolean("warningShown", true).apply();
                dialog.dismiss();
            }
        });
        builder.setCancelable(false);
        builder.show();
    }

    private void requestBatteryExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            try {
                startActivity(intent);
            } catch (Exception ignored) {}
        }
    }

    private boolean isAccessibilityServiceEnabled() {
        String service = getPackageName() + "/" + AudioRoutingService.class.getCanonicalName();
        try {
            String enabledServices = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            return enabledServices != null && enabledServices.contains(service);
        } catch (Exception ignored) {
            return false;
        }
    }

    private void startAudioRouting() {
        MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
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
            toggleButton.setText("ON\n💙");
            toggleButton.setBackgroundResource(R.drawable.glacier_button_on);
            toggleButton.setTextColor(0xFFFFFFFF);
            statusText.setText("⚡ STATUS: ACTIVE - MIC SHARING");
            glowOverlay.setVisibility(View.VISIBLE);
            
            // Start snowfall effect
            startSnowfall();
            
            Toast.makeText(this, "❄️ Audio routing active! Snowfall started! ❄️", Toast.LENGTH_LONG).show();
        }
    }

    private void stopAudioRouting() {
        stopService(new Intent(this, ForegroundMicService.class));
        isActive = false;
        toggleButton.setText("OFF\n❄️");
        toggleButton.setBackgroundResource(R.drawable.glacier_button_off);
        toggleButton.setTextColor(0xFF88AACC);
        statusText.setText("⚡ STATUS: IDLE");
        glowOverlay.setVisibility(View.GONE);
        
        // Stop snowfall
        stopSnowfall();
        
        Toast.makeText(this, "Audio routing stopped.", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isActive) {
            stopAudioRouting();
        }
        stopSnowfall();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_MICROPHONE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Microphone permission granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Microphone permission is required for mic sharing", Toast.LENGTH_LONG).show();
            }
        }
    }
}