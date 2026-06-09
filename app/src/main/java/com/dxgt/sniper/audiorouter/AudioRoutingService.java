package com.dxgt.sniper.audiorouter;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.view.accessibility.AccessibilityEvent;

public class AudioRoutingService extends AccessibilityService {
    
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Service exists to grant privileged system access
        // No additional logic needed - presence enables mic sharing
    }

    @Override
    public void onInterrupt() {
        // Required override
    }
    
    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        // Configure service
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        setServiceInfo(info);
    }
}