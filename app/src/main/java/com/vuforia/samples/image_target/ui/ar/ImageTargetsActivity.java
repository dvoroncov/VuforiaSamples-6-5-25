package com.vuforia.samples.image_target.ui.ar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Switch;

import com.vuforia.CameraDevice;
import com.vuforia.DataSet;
import com.vuforia.ObjectTracker;
import com.vuforia.STORAGE_TYPE;
import com.vuforia.State;
import com.vuforia.Trackable;
import com.vuforia.Tracker;
import com.vuforia.TrackerManager;
import com.vuforia.Vuforia;
import com.vuforia.samples.image_target.R;
import com.vuforia.samples.image_target.ar_session.ARSession;
import com.vuforia.samples.image_target.ar_session.ARSessionControl;
import com.vuforia.samples.image_target.renderer.ImageTargetRenderer;
import com.vuforia.samples.image_target.utils.ARException;
import com.vuforia.samples.image_target.utils.ARGLSurfaceView;
import com.vuforia.samples.image_target.utils.LoadingDialogHandler;
import com.vuforia.samples.image_target.utils.Texture;

import java.util.ArrayList;
import java.util.Vector;

import io.github.controlwear.virtual.joystick.android.JoystickView;

public class ImageTargetsActivity extends Activity implements ARSessionControl {

    private static final String LOGTAG = ImageTargetsActivity.class.getSimpleName();
    private static final String DROID = "droid";
    private static final String IMAGE_TARGET_DRAGON = "ImageTargets/Dragon.xml";
    private static final String TEXTURE_AMENEMHAT = "3dModels/amenemhat.jpg";
    private static final String TEXTURE_SHOTGUN = "3dModels/Sg_Diffuse.png";
    private static final String TEXTURE_TURRET = "3dModels/tex_metal_combinated.jpg";

    boolean isDroidDevice = false;

    private ARSession vuforiaAppSession;
    private DataSet currentDataset;
    private ArrayList<String> datasetStrings = new ArrayList<>();
    private ARGLSurfaceView ARGLSurfaceView;
    private ImageTargetRenderer renderer;
    private Vector<Texture> textures;
    private boolean switchDatasetAsap = false;
    private boolean contAutofocus = true;
    private RelativeLayout relativeLayout;
    private LoadingDialogHandler loadingDialogHandler = new LoadingDialogHandler(this);
    private AlertDialog errorDialog;
    private JoystickView joystickView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOGTAG, "onCreate");
        super.onCreate(savedInstanceState);

        vuforiaAppSession = new ARSession(this);

        startLoadingAnimation();

        joystickView = findViewById(R.id.joystick);

        datasetStrings.add(IMAGE_TARGET_DRAGON);

        vuforiaAppSession.initAR(this, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        textures = new Vector<>();
        loadTextures(TEXTURE_TURRET);

        isDroidDevice = android.os.Build.MODEL.toLowerCase().startsWith(DROID);
    }

    private void loadTextures(String fileName) {
        textures.add(Texture.loadTextureFromApk(fileName,
                getAssets()));
    }

    @Override
    protected void onResume() {
        Log.d(LOGTAG, "onResume");
        super.onResume();

        showProgressIndicator(true);

        if (isDroidDevice) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        vuforiaAppSession.onResume();
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        Log.d(LOGTAG, "onConfigurationChanged");
        super.onConfigurationChanged(config);

        vuforiaAppSession.onConfigurationChanged();
    }

    @Override
    protected void onPause() {
        Log.d(LOGTAG, "onPause");
        super.onPause();

        if (ARGLSurfaceView != null) {
            ARGLSurfaceView.setVisibility(View.INVISIBLE);
            ARGLSurfaceView.onPause();
        }

        try {
            vuforiaAppSession.pauseAR();
        } catch (ARException e) {
            Log.e(LOGTAG, e.getDescription());
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(LOGTAG, "onDestroy");
        super.onDestroy();

        try {
            vuforiaAppSession.stopAR();
        } catch (ARException e) {
            Log.e(LOGTAG, e.getDescription());
        }

        textures.clear();
        textures = null;

        System.gc();
    }

    private void initApplicationAR() {
        int depthSize = 16;
        int stencilSize = 0;
        boolean translucent = Vuforia.requiresAlpha();

        ARGLSurfaceView = new ARGLSurfaceView(this);
        ARGLSurfaceView.init(translucent, depthSize, stencilSize);

        renderer = new ImageTargetRenderer(this, vuforiaAppSession);
        renderer.setTextures(textures);
        ARGLSurfaceView.setRenderer(renderer);

        joystickView.setOnMoveListener(renderer);
    }


    private void startLoadingAnimation() {
        relativeLayout = (RelativeLayout) View.inflate(this, R.layout.camera_overlay, null);

        relativeLayout.setVisibility(View.VISIBLE);
        relativeLayout.setBackgroundColor(Color.BLACK);

        loadingDialogHandler.loadingDialogContainer = relativeLayout
                .findViewById(R.id.loading_indicator);
        loadingDialogHandler
                .sendEmptyMessage(LoadingDialogHandler.SHOW_LOADING_DIALOG);

        addContentView(relativeLayout, new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));

    }

    @Override
    public boolean doLoadTrackersData() {
        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) trackerManager
                .getTracker(ObjectTracker.getClassType());
        if (objectTracker == null) {
            return false;
        }

        if (currentDataset == null) {
            currentDataset = objectTracker.createDataSet();
        }

        if (currentDataset == null) {
            return false;
        }

        if (!currentDataset.load(
                datasetStrings.get(0),
                STORAGE_TYPE.STORAGE_APPRESOURCE)) {
            return false;
        }

        if (!objectTracker.activateDataSet(currentDataset)) {
            return false;
        }

        int numTrackables = currentDataset.getNumTrackables();
        for (int count = 0; count < numTrackables; count++) {
            Trackable trackable = currentDataset.getTrackable(count);

            String name = "Current Dataset : " + trackable.getName();
            trackable.setUserData(name);
            Log.d(LOGTAG, "UserData:Set the following user data "
                    + trackable.getUserData());
        }

        return true;
    }


    @Override
    public boolean doUnloadTrackersData() {
        boolean result = true;

        TrackerManager tManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) tManager
                .getTracker(ObjectTracker.getClassType());
        if (objectTracker == null) {
            return false;
        }

        if (currentDataset != null && currentDataset.isActive()) {
            if (objectTracker.getActiveDataSet(0).equals(currentDataset)
                    && !objectTracker.deactivateDataSet(currentDataset)) {
                result = false;
            } else if (!objectTracker.destroyDataSet(currentDataset)) {
                result = false;
            }

            currentDataset = null;
        }

        return result;
    }

    @Override
    public void onVuforiaResumed() {
        if (ARGLSurfaceView != null) {
            ARGLSurfaceView.setVisibility(View.VISIBLE);
            ARGLSurfaceView.onResume();
        }
    }

    @Override
    public void onVuforiaStarted() {
        renderer.updateConfiguration();

        if (contAutofocus) {
            if (!CameraDevice
                    .getInstance()
                    .setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO)) {
                if (!CameraDevice
                        .getInstance()
                        .setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_TRIGGERAUTO)) {
                    CameraDevice
                            .getInstance()
                            .setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_NORMAL);
                }
            }
        }

        showProgressIndicator(false);
    }


    public void showProgressIndicator(boolean show) {
        if (loadingDialogHandler != null) {
            if (show) {
                loadingDialogHandler.sendEmptyMessage(LoadingDialogHandler.SHOW_LOADING_DIALOG);
            } else {
                loadingDialogHandler.sendEmptyMessage(LoadingDialogHandler.HIDE_LOADING_DIALOG);
            }
        }
    }


    @Override
    public void onInitARDone(ARException exception) {
        if (exception == null) {
            initApplicationAR();

            renderer.setActive(true);

            addContentView(ARGLSurfaceView, new LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT));
            relativeLayout.bringToFront();
            relativeLayout.setBackgroundColor(Color.TRANSPARENT);

            vuforiaAppSession.startAR(CameraDevice.CAMERA_DIRECTION.CAMERA_DIRECTION_DEFAULT);
        } else {
            Log.e(LOGTAG, exception.getDescription());
            showInitializationErrorMessage(exception.getDescription());
        }
    }

    public void showInitializationErrorMessage(String message) {
        final String errorMessage = message;
        runOnUiThread(new Runnable() {
            public void run() {
                if (errorDialog != null) {
                    errorDialog.dismiss();
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(
                        ImageTargetsActivity.this);
                builder
                        .setMessage(errorMessage)
                        .setTitle(getString(R.string.INIT_ERROR))
                        .setCancelable(false)
                        .setIcon(0)
                        .setPositiveButton(getString(R.string.button_OK),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        finish();
                                    }
                                });

                errorDialog = builder.create();
                errorDialog.show();
            }
        });
    }

    @Override
    public void onVuforiaUpdate(State state) {
        if (switchDatasetAsap) {
            switchDatasetAsap = false;
            TrackerManager trackerManager = TrackerManager.getInstance();
            ObjectTracker objectTracker = (ObjectTracker) trackerManager.getTracker(ObjectTracker.getClassType());
            if (objectTracker == null
                    || currentDataset == null
                    || objectTracker.getActiveDataSet(0) == null) {
                Log.d(LOGTAG, "Failed to swap datasets");
                return;
            }

            doUnloadTrackersData();
            doLoadTrackersData();
        }
    }

    @Override
    public boolean doInitTrackers() {
        TrackerManager tManager = TrackerManager.getInstance();
        Tracker tracker;

        tracker = tManager.initTracker(ObjectTracker.getClassType());
        if (tracker == null) {
            Log.e(LOGTAG, "Tracker not initialized. Tracker already initialized or the camera is already started");
            return false;
        } else {
            Log.i(LOGTAG, "Tracker successfully initialized");
        }
        return true;
    }

    @Override
    public void doStartTrackers() {
        Tracker objectTracker = TrackerManager.getInstance().getTracker(
                ObjectTracker.getClassType());
        if (objectTracker != null) {
            objectTracker.start();
        }

    }

    @Override
    public void doStopTrackers() {
        Tracker objectTracker = TrackerManager.getInstance().getTracker(
                ObjectTracker.getClassType());
        if (objectTracker != null) {
            objectTracker.stop();
        }

    }

    @Override
    public boolean doDeinitTrackers() {
        TrackerManager trackerManager = TrackerManager.getInstance();
        trackerManager.deinitTracker(ObjectTracker.getClassType());

        return true;
    }

    public LoadingDialogHandler getLoadingDialogHandler() {
        return loadingDialogHandler;
    }
}
