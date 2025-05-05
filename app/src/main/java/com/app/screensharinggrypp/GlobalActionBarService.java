package com.app.screensharinggrypp;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.lifecycle.ProcessLifecycleOwner;

import com.app.screenshare.sharingMain.ConnectingSession;


public class GlobalActionBarService extends Service {

    private WindowManager windowManager;
    private Button overlayButton;
    private WindowManager.LayoutParams params;
    private LinearLayout dialogLayout;
    private WindowManager.LayoutParams dialogParams;
    private AppLifecycleObserver lifecycleObserver;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize lifecycle observer to stop service when app is in background
        lifecycleObserver = new AppLifecycleObserver(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(lifecycleObserver);

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        overlayButton = new Button(this);
        overlayButton.setText("GRYPP");
        overlayButton.setTextColor(0xFFFFFFFF); // White text
        overlayButton.setPadding(20, 5, 20, 5);
        overlayButton.setBackgroundResource(R.drawable.round_button_bg);

        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 180;
        params.y = 20;

        // Make the button draggable
        overlayButton.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(overlayButton, params);
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (Math.abs(event.getRawX() - initialTouchX) < 5 && Math.abs(event.getRawY() - initialTouchY) < 5) {
                            v.performClick();
                        }
                        return true;
                }
                return false;
            }
        });

        // Add click action
        overlayButton.setOnClickListener(v -> {
            if (MainApplication.Companion.getScreenShareComponent().getStatus_ScreenShare() == 0) {
                // Start screen sharing and show session code dialog
                ConnectingSession session = new ConnectingSession() {
                    @Override
                    public void onSessionDisconnected(boolean isConnected) {
                        overlayButton.setText("GRYPP");
                    }

                    @Override
                    public void onSessionConnected(boolean isConnected) {
                        // Remove dialog when session is connected
                        removeDialog();
                        overlayButton.setText("Capturing Screen");
                    }

                    @Override
                    public void onSessionCode(@NonNull String sessionCode) {
                        showSessionCodeDialog(sessionCode);
                    }
                };
                MainApplication.Companion.getScreenShareComponent().startScreenShare(session);
            } else {
                // Show end session dialog
                showEndSessionDialog();
            }
        });

        windowManager.addView(overlayButton, params);
    }

    private void showSessionCodeDialog(String sessionCode) {
        // Create dialog layout
        dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setBackgroundColor(0xFFFFFFFF); // White background
        dialogLayout.setPadding(20, 20, 20, 20);

        // Title
        TextView title = new TextView(this);
        title.setText("Connect Session");
        title.setTextColor(0xFF000000);
        title.setTextSize(20);
        title.setPadding(0, 0, 0, 20);
        dialogLayout.addView(title);

        // Message
        TextView message = new TextView(this);
        message.setText("Your session code is " + sessionCode);
        message.setTextColor(0xFF000000);
        message.setPadding(0, 0, 0, 20);
        dialogLayout.addView(message);

        // Cancel button
        Button cancelButton = new Button(this);
        cancelButton.setText("Cancel");
        cancelButton.setTextColor(0xFFFFFFFF);
        cancelButton.setBackgroundResource(R.drawable.round_btn_red);
        cancelButton.setPadding(10, 10, 10, 20);
        cancelButton.setOnClickListener(v -> {
            MainApplication.Companion.getScreenShareComponent().stopScreenShare();
            removeDialog();
        });
        dialogLayout.addView(cancelButton);

        // Dialog layout params
        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }

        dialogParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                PixelFormat.TRANSLUCENT);

        dialogParams.gravity = Gravity.CENTER;
        dialogParams.dimAmount = 0.5f; // Dim the background

        // Add dialog to window
        windowManager.addView(dialogLayout, dialogParams);
    }

    private void showEndSessionDialog() {
        // Create dialog layout
        dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setBackgroundColor(0xFFFFFFFF); // White background
        dialogLayout.setPadding(20, 20, 20, 20);

        // Title
        TextView title = new TextView(this);
        title.setText("End Session");
        title.setTextColor(0xFF000000);
        title.setTextSize(20);
        title.setPadding(20, 10, 20, 20);
        dialogLayout.addView(title);

        // Message
        TextView message = new TextView(this);
        message.setText("Do you want to end the current session?");
        message.setTextColor(0xFF000000);
        dialogLayout.addView(message);

        // Buttons layout
        LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonLayout.setGravity(Gravity.END);

        // Yes button
        Button yesButton = new Button(this);
        yesButton.setText("Yes");
        yesButton.setTextColor(0xFFFFFFFF);
        yesButton.setBackgroundResource(R.drawable.round_button_bg);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        buttonParams.setMargins(10, 10, 10, 10); // 10dp margins
        yesButton.setLayoutParams(buttonParams);
        yesButton.setOnClickListener(v -> {
            overlayButton.setText("GRYPP");
            MainApplication.Companion.getScreenShareComponent().stopScreenShare();
            removeDialog();
        });
        buttonLayout.addView(yesButton);

        // No button
        Button noButton = new Button(this);
        noButton.setText("No");
        noButton.setTextColor(0xFFFFFFFF);
        noButton.setBackgroundResource(R.drawable.round_btn_red);
        noButton.setPadding(40, 10, 10, 20);
        noButton.setOnClickListener(v -> removeDialog());
        noButton.setLayoutParams(buttonParams);
        buttonLayout.addView(noButton);

        dialogLayout.addView(buttonLayout);

        // Dialog layout params
        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }

        dialogParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                PixelFormat.TRANSLUCENT);

        dialogParams.gravity = Gravity.CENTER;
        dialogParams.dimAmount = 0.5f; // Dim the background

        // Add dialog to window
        windowManager.addView(dialogLayout, dialogParams);
    }

    private void removeDialog() {
        if (dialogLayout != null) {
            windowManager.removeView(dialogLayout);
            dialogLayout = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (overlayButton != null) {
            windowManager.removeView(overlayButton);
            overlayButton = null;
        }
        removeDialog(); // Ensure dialog is removed when service stops
        windowManager = null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopSelf();
    }


    public void hideOverlayButton() {
        removeDialog();
        if (MainApplication.Companion.getScreenShareComponent().getStatus_ScreenShare() != 0) {
            MainApplication.Companion.getScreenShareComponent().stopScreenShare();
        }
        if (overlayButton != null && overlayButton.getWindowToken() != null) {
            overlayButton.setVisibility(View.GONE);
        }
    }

    public void showOverlayButton() {
        if (overlayButton != null) {
            overlayButton.setVisibility(View.VISIBLE);
        }
    }
}