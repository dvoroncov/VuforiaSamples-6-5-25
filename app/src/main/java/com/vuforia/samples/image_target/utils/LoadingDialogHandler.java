package com.vuforia.samples.image_target.utils;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.view.View;

import java.lang.ref.WeakReference;

public final class LoadingDialogHandler extends Handler {

    public static final int HIDE_LOADING_DIALOG = 0;
    public static final int SHOW_LOADING_DIALOG = 1;

    private final WeakReference<Activity> activity;

    public View loadingDialogContainer;

    public LoadingDialogHandler(Activity activity) {
        this.activity = new WeakReference<>(activity);
    }

    public void handleMessage(Message message) {
        Activity imageTargets = activity.get();
        if (imageTargets == null) {
            return;
        }

        if (message.what == SHOW_LOADING_DIALOG) {
            loadingDialogContainer.setVisibility(View.VISIBLE);

        } else if (message.what == HIDE_LOADING_DIALOG) {
            loadingDialogContainer.setVisibility(View.GONE);
        }
    }
}
