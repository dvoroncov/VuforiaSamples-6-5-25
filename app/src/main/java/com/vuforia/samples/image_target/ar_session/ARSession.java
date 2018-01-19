package com.vuforia.samples.image_target.ar_session;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.WindowManager;

import com.vuforia.CameraDevice;
import com.vuforia.Device;
import com.vuforia.INIT_ERRORCODE;
import com.vuforia.INIT_FLAGS;
import com.vuforia.State;
import com.vuforia.Vuforia;
import com.vuforia.Vuforia.UpdateCallbackInterface;
import com.vuforia.samples.image_target.R;
import com.vuforia.samples.image_target.utils.ARException;

public class ARSession implements UpdateCallbackInterface {

    private static final String LOGTAG = ARSession.class.getSimpleName();

    private Activity activity;
    private ARSessionControl sessionControl;

    private boolean isStarted = false;
    private boolean isCameraRunning = false;

    private InitVuforiaTask initVuforiaTask;
    private InitTrackerTask initTrackerTask;
    private LoadTrackerTask loadTrackerTask;
    private StartVuforiaTask startVuforiaTask;
    private ResumeVuforiaTask resumeVuforiaTask;

    private final Object lifecycleLock = new Object();

    private int vuforiaFlags = 0;
    private int cameraDirection = CameraDevice.CAMERA_DIRECTION.CAMERA_DIRECTION_DEFAULT;


    public ARSession(ARSessionControl sessionControl) {
        this.sessionControl = sessionControl;
    }

    public void initAR(Activity activity, int screenOrientation) {
        ARException vuforiaException = null;
        this.activity = activity;

        if (screenOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR) {
            screenOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR;
        }

        OrientationEventListener orientationEventListener = new OrientationEventListener(ARSession.this.activity) {

            int lastRotation = -1;

            @Override
            public void onOrientationChanged(int i) {
                int activityRotation = ARSession.this.activity.getWindowManager().getDefaultDisplay().getRotation();
                if (lastRotation != activityRotation) {
                    lastRotation = activityRotation;
                }
            }
        };

        if (orientationEventListener.canDetectOrientation()) {
            orientationEventListener.enable();
        }

        this.activity.setRequestedOrientation(screenOrientation);
        this.activity.getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        vuforiaFlags = INIT_FLAGS.GL_20;

        if (initVuforiaTask != null) {
            String logMessage = activity.getString(R.string.cannot_initialize_sdk_twice);
            vuforiaException = new ARException(
                    ARException.VUFORIA_ALREADY_INITIALIZED,
                    logMessage);
            Log.e(LOGTAG, logMessage);
        }

        if (vuforiaException == null) {
            try {
                initVuforiaTask = new InitVuforiaTask();
                initVuforiaTask.execute();
            } catch (Exception e) {
                String logMessage = activity.getString(R.string.initializing_vuforia_sdk_failed);
                vuforiaException = new ARException(
                        ARException.INITIALIZATION_FAILURE,
                        logMessage);
                Log.e(LOGTAG, logMessage);
            }
        }

        if (vuforiaException != null) {
            sessionControl.onInitARDone(vuforiaException);
        }
    }

    private void startCameraAndTrackers(int camera) throws ARException {
        String error;
        if (isCameraRunning) {
            error = "Camera already running, unable to open again";
            Log.e(LOGTAG, error);
            throw new ARException(
                    ARException.CAMERA_INITIALIZATION_FAILURE, error);
        }

        this.cameraDirection = camera;
        if (!CameraDevice.getInstance().init(camera)) {
            error = "Unable to open cameraDirection device: " + camera;
            Log.e(LOGTAG, error);
            throw new ARException(
                    ARException.CAMERA_INITIALIZATION_FAILURE, error);
        }

        if (!CameraDevice.getInstance().selectVideoMode(
                CameraDevice.MODE.MODE_DEFAULT)) {
            error = "Unable to set video mode";
            Log.e(LOGTAG, error);
            throw new ARException(
                    ARException.CAMERA_INITIALIZATION_FAILURE, error);
        }

        if (!CameraDevice.getInstance().start()) {
            error = "Unable to start cameraDirection device: " + camera;
            Log.e(LOGTAG, error);
            throw new ARException(
                    ARException.CAMERA_INITIALIZATION_FAILURE, error);
        }

        sessionControl.doStartTrackers();

        isCameraRunning = true;
    }

    public void startAR(int camera) {
        this.cameraDirection = camera;
        ARException vuforiaException = null;

        try {
            startVuforiaTask = new StartVuforiaTask();
            startVuforiaTask.execute();
        } catch (Exception e) {
            String logMessage = "Starting Vuforia failed";
            vuforiaException = new ARException(
                    ARException.CAMERA_INITIALIZATION_FAILURE,
                    logMessage);
            Log.e(LOGTAG, logMessage);
        }

        if (vuforiaException != null) {
            sessionControl.onInitARDone(vuforiaException);
        }
    }

    public void stopAR() throws ARException {
        if (initVuforiaTask != null
                && initVuforiaTask.getStatus() != InitVuforiaTask.Status.FINISHED) {
            initVuforiaTask.cancel(true);
            initVuforiaTask = null;
        }

        if (loadTrackerTask != null
                && loadTrackerTask.getStatus() != LoadTrackerTask.Status.FINISHED) {
            loadTrackerTask.cancel(true);
            loadTrackerTask = null;
        }

        initVuforiaTask = null;
        loadTrackerTask = null;

        isStarted = false;

        stopCamera();

        synchronized (lifecycleLock) {

            boolean unloadTrackersResult;
            boolean deinitTrackersResult;

            unloadTrackersResult = sessionControl.doUnloadTrackersData();
            deinitTrackersResult = sessionControl.doDeinitTrackers();
            Vuforia.deinit();

            if (!unloadTrackersResult) {
                throw new ARException(
                        ARException.UNLOADING_TRACKERS_FAILURE,
                        activity.getString(R.string.failed_to_unload_trackers_data));
            }

            if (!deinitTrackersResult) {
                throw new ARException(
                        ARException.TRACKERS_DEINITIALIZATION_FAILURE,
                        activity.getString(R.string.failed_to_deinitialize_trackers));
            }

        }
    }

    private void resumeAR() {
        ARException vuforiaException = null;

        try {
            resumeVuforiaTask = new ResumeVuforiaTask();
            resumeVuforiaTask.execute();
        } catch (Exception e) {
            String logMessage = activity.getString(R.string.resuming_vuforia_failed);
            vuforiaException = new ARException(
                    ARException.INITIALIZATION_FAILURE,
                    logMessage);
            Log.e(LOGTAG, logMessage);
        }

        if (vuforiaException != null) {
            sessionControl.onInitARDone(vuforiaException);
        }
    }

    public void pauseAR() throws ARException {
        if (isStarted) {
            stopCamera();
        }

        Vuforia.onPause();
    }

    @Override
    public void Vuforia_onUpdate(State s) {
        sessionControl.onVuforiaUpdate(s);
    }

    public void onConfigurationChanged() {
        Device.getInstance().setConfigurationChanged();
    }

    public void onResume() {
        if (resumeVuforiaTask == null
                || resumeVuforiaTask.getStatus() == ResumeVuforiaTask.Status.FINISHED) {
            resumeAR();
        }
    }

    public void onPause() {
        Vuforia.onPause();
    }

    public void onSurfaceChanged(int width, int height) {
        Vuforia.onSurfaceChanged(width, height);
    }

    public void onSurfaceCreated() {
        Vuforia.onSurfaceCreated();
    }

    private class InitVuforiaTask extends AsyncTask<Void, Integer, Boolean> {

        private int mProgressValue = -1;

        protected Boolean doInBackground(Void... params) {
            synchronized (lifecycleLock) {
                Vuforia.setInitParameters(activity, vuforiaFlags, activity.getString(R.string.vuforia_key));

                do {
                    mProgressValue = Vuforia.init();
                    publishProgress(mProgressValue);
                } while (!isCancelled() && mProgressValue >= 0
                        && mProgressValue < 100);

                return (mProgressValue > 0);
            }
        }


        protected void onProgressUpdate(Integer... values) {
        }


        protected void onPostExecute(Boolean result) {
            Log.d(LOGTAG, "InitVuforiaTask.onPostExecute: execution "
                    + (result ? "successful" : "failed"));

            ARException vuforiaException = null;

            if (result) {
                try {
                    initTrackerTask = new InitTrackerTask();
                    initTrackerTask.execute();
                } catch (Exception e) {
                    String logMessage = activity.getString(R.string.failed_to_initialize_tracker);
                    vuforiaException = new ARException(
                            ARException.TRACKERS_INITIALIZATION_FAILURE,
                            logMessage);
                    Log.e(LOGTAG, logMessage);
                }
            } else {
                String logMessage;
                logMessage = getInitializationErrorString(mProgressValue);

                Log.e(LOGTAG, "InitVuforiaTask.onPostExecute: " + logMessage
                        + " Exiting.");

                vuforiaException = new ARException(
                        ARException.INITIALIZATION_FAILURE,
                        logMessage);
            }

            if (vuforiaException != null) {
                sessionControl.onInitARDone(vuforiaException);
            }
        }
    }

    private class ResumeVuforiaTask extends AsyncTask<Void, Void, Void> {

        protected Void doInBackground(Void... params) {
            synchronized (lifecycleLock) {
                Vuforia.onResume();
            }

            return null;
        }

        protected void onPostExecute(Void result) {
            Log.d(LOGTAG, "ResumeVuforiaTask.onPostExecute");

            if (isStarted && !isCameraRunning) {
                startAR(cameraDirection);
                sessionControl.onVuforiaResumed();
            }
        }
    }

    private class InitTrackerTask extends AsyncTask<Void, Integer, Boolean> {

        protected Boolean doInBackground(Void... params) {
            synchronized (lifecycleLock) {
                return sessionControl.doInitTrackers();
            }
        }

        protected void onPostExecute(Boolean result) {

            ARException vuforiaException = null;
            Log.d(LOGTAG, "InitTrackerTask.onPostExecute: execution "
                    + (result ? "successful" : "failed"));

            if (result) {
                try {
                    loadTrackerTask = new LoadTrackerTask();
                    loadTrackerTask.execute();
                } catch (Exception e) {
                    String logMessage = "Failed to load tracker data.";
                    Log.e(LOGTAG, logMessage);

                    vuforiaException = new ARException(
                            ARException.LOADING_TRACKERS_FAILURE,
                            logMessage);
                }
            } else {
                String logMessage = "Failed to load tracker data.";
                Log.e(LOGTAG, logMessage);

                vuforiaException = new ARException(
                        ARException.TRACKERS_INITIALIZATION_FAILURE,
                        logMessage);
            }

            if (vuforiaException != null) {
                sessionControl.onInitARDone(vuforiaException);
            }
        }
    }

    private class LoadTrackerTask extends AsyncTask<Void, Void, Boolean> {

        protected Boolean doInBackground(Void... params) {
            synchronized (lifecycleLock) {
                return sessionControl.doLoadTrackersData();
            }
        }

        protected void onPostExecute(Boolean result) {

            ARException vuforiaException = null;

            Log.d(LOGTAG, "LoadTrackerTask.onPostExecute: execution "
                    + (result ? "successful" : "failed"));

            if (!result) {
                String logMessage = "Failed to load tracker data.";
                Log.e(LOGTAG, logMessage);
                vuforiaException = new ARException(
                        ARException.LOADING_TRACKERS_FAILURE,
                        logMessage);
            } else {
                System.gc();

                Vuforia.registerCallback(ARSession.this);

                isStarted = true;
            }

            sessionControl.onInitARDone(vuforiaException);
        }
    }

    private class StartVuforiaTask extends AsyncTask<Void, Void, Boolean> {

        private ARException vuforiaException = null;

        protected Boolean doInBackground(Void... params) {
            synchronized (lifecycleLock) {
                try {
                    startCameraAndTrackers(cameraDirection);
                } catch (ARException e) {
                    Log.e(LOGTAG, "StartVuforiaTask.doInBackground: Could not start AR with exception: " + e);
                    vuforiaException = e;
                }
            }

            return true;
        }

        protected void onPostExecute(Boolean result) {
            Log.d(LOGTAG, "StartVuforiaTask.onPostExecute: execution "
                    + (result ? "successful" : "failed"));

            sessionControl.onVuforiaStarted();

            if (vuforiaException != null) {
                sessionControl.onInitARDone(vuforiaException);
            }
        }
    }

    private String getInitializationErrorString(int code) {
        if (code == INIT_ERRORCODE.INIT_DEVICE_NOT_SUPPORTED)
            return activity.getString(R.string.INIT_ERROR_DEVICE_NOT_SUPPORTED);
        if (code == INIT_ERRORCODE.INIT_NO_CAMERA_ACCESS)
            return activity.getString(R.string.INIT_ERROR_NO_CAMERA_ACCESS);
        if (code == INIT_ERRORCODE.INIT_LICENSE_ERROR_MISSING_KEY)
            return activity.getString(R.string.INIT_LICENSE_ERROR_MISSING_KEY);
        if (code == INIT_ERRORCODE.INIT_LICENSE_ERROR_INVALID_KEY)
            return activity.getString(R.string.INIT_LICENSE_ERROR_INVALID_KEY);
        if (code == INIT_ERRORCODE.INIT_LICENSE_ERROR_NO_NETWORK_TRANSIENT)
            return activity.getString(R.string.INIT_LICENSE_ERROR_NO_NETWORK_TRANSIENT);
        if (code == INIT_ERRORCODE.INIT_LICENSE_ERROR_NO_NETWORK_PERMANENT)
            return activity.getString(R.string.INIT_LICENSE_ERROR_NO_NETWORK_PERMANENT);
        if (code == INIT_ERRORCODE.INIT_LICENSE_ERROR_CANCELED_KEY)
            return activity.getString(R.string.INIT_LICENSE_ERROR_CANCELED_KEY);
        if (code == INIT_ERRORCODE.INIT_LICENSE_ERROR_PRODUCT_TYPE_MISMATCH)
            return activity.getString(R.string.INIT_LICENSE_ERROR_PRODUCT_TYPE_MISMATCH);
        else {
            return activity.getString(R.string.INIT_LICENSE_ERROR_UNKNOWN_ERROR);
        }
    }

    private void stopCamera() {
        if (isCameraRunning) {
            sessionControl.doStopTrackers();
            isCameraRunning = false;
            CameraDevice.getInstance().stop();
            CameraDevice.getInstance().deinit();
        }
    }

    private boolean isARRunning() {
        return isStarted;
    }

}
