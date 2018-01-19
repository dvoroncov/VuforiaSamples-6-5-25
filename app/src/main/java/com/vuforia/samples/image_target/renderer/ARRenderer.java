package com.vuforia.samples.image_target.renderer;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Point;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Build;
import android.util.Log;

import com.vuforia.COORDINATE_SYSTEM_TYPE;
import com.vuforia.CameraDevice;
import com.vuforia.Device;
import com.vuforia.GLTextureUnit;
import com.vuforia.Matrix34F;
import com.vuforia.Mesh;
import com.vuforia.Renderer;
import com.vuforia.RenderingPrimitives;
import com.vuforia.State;
import com.vuforia.Tool;
import com.vuforia.TrackerManager;
import com.vuforia.VIDEO_BACKGROUND_REFLECTION;
import com.vuforia.VIEW;
import com.vuforia.Vec2F;
import com.vuforia.Vec2I;
import com.vuforia.Vec4I;
import com.vuforia.VideoBackgroundConfig;
import com.vuforia.VideoMode;
import com.vuforia.ViewList;
import com.vuforia.samples.image_target.R;
import com.vuforia.samples.image_target.utils.Utils;
import com.vuforia.samples.image_target.renderer.shaders.VideoBackgroundShaders;

public class ARRenderer {

    private static final String LOGTAG = ARRenderer.class.getSimpleName();
    private static final float VIRTUAL_FOV_Y_DEGS = 85.0f;
    private static final float M_PI = 3.14159f;

    private RenderingPrimitives renderingPrimitives = null;
    private ARRendererControl renderingInterface = null;
    private Activity activity = null;

    private Renderer renderer = null;
    private int currentView = VIEW.VIEW_SINGULAR;
    private float nearPlane = -1.0f;
    private float farPlane = -1.0f;

    private GLTextureUnit videoBackgroundTexture = null;

    private int vbShaderProgramID = 0;
    private int vbTexSampler2DHandle = 0;
    private int vbVertexHandle = 0;
    private int vbTexCoordHandle = 0;
    private int vbProjectionMatrixHandle = 0;

    private int screenWidth = 0;
    private int screenHeight = 0;

    private boolean isPortrait = false;

    public ARRenderer(ARRendererControl renderingInterface,
                      Activity activity,
                      int deviceMode,
                      boolean stereo,
                      float nearPlane,
                      float farPlane) {
        this.activity = activity;
        this.renderingInterface = renderingInterface;
        renderer = Renderer.getInstance();

        if (farPlane < nearPlane) {
            Log.e(LOGTAG, "Far plane should be greater than near plane");
            throw new IllegalArgumentException();
        }

        setNearFarPlanes(nearPlane, farPlane);

        if (deviceMode != Device.MODE.MODE_AR && deviceMode != Device.MODE.MODE_VR) {
            Log.e(LOGTAG, activity.getString(R.string.incorrect_device_mode));
            throw new IllegalArgumentException();
        }

        Device device = Device.getInstance();
        device.setViewerActive(stereo);
        device.setMode(deviceMode);
    }

    public void onSurfaceCreated() {
        initRendering();
    }

    public void onConfigurationChanged(boolean isARActive) {
        updateActivityOrientation();
        storeScreenDimensions();

        if (isARActive)
            configureVideoBackground();

        renderingPrimitives = Device.getInstance().getRenderingPrimitives();
    }

    private void initRendering() {
        vbShaderProgramID = Utils.createProgramFromShaderSrc(VideoBackgroundShaders.VB_VERTEX_SHADER,
                VideoBackgroundShaders.VB_FRAGMENT_SHADER);

        if (vbShaderProgramID > 0) {
            GLES20.glUseProgram(vbShaderProgramID);

            vbTexSampler2DHandle = GLES20.glGetUniformLocation(vbShaderProgramID, "texSampler2D");

            vbProjectionMatrixHandle = GLES20.glGetUniformLocation(vbShaderProgramID, "projectionMatrix");

            vbVertexHandle = GLES20.glGetAttribLocation(vbShaderProgramID, "vertexPosition");
            vbTexCoordHandle = GLES20.glGetAttribLocation(vbShaderProgramID, "vertexTexCoord");
            vbProjectionMatrixHandle = GLES20.glGetUniformLocation(vbShaderProgramID, "projectionMatrix");
            vbTexSampler2DHandle = GLES20.glGetUniformLocation(vbShaderProgramID, "texSampler2D");

            GLES20.glUseProgram(0);
        }

        videoBackgroundTexture = new GLTextureUnit();
    }

    public void render() {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        State state;
        state = TrackerManager.getInstance().getStateUpdater().updateState();
        renderer.begin(state);

        if (Renderer.getInstance().getVideoBackgroundConfig().getReflection() == VIDEO_BACKGROUND_REFLECTION.VIDEO_BACKGROUND_REFLECTION_ON) {
            GLES20.glFrontFace(GLES20.GL_CW);
        } else {
            GLES20.glFrontFace(GLES20.GL_CCW);
        }

        ViewList viewList = renderingPrimitives.getRenderingViews();

        for (int v = 0; v < viewList.getNumViews(); v++) {
            int viewID = viewList.getView(v);

            Vec4I viewport;
            viewport = renderingPrimitives.getViewport(viewID);

            GLES20.glViewport(viewport.getData()[0], viewport.getData()[1], viewport.getData()[2], viewport.getData()[3]);

            GLES20.glScissor(viewport.getData()[0], viewport.getData()[1], viewport.getData()[2], viewport.getData()[3]);

            Matrix34F projMatrix = renderingPrimitives.getProjectionMatrix(viewID, COORDINATE_SYSTEM_TYPE.COORDINATE_SYSTEM_CAMERA);

            float rawProjectionMatrixGL[] = Tool.convertPerspectiveProjection2GLMatrix(
                    projMatrix,
                    nearPlane,
                    farPlane)
                    .getData();

            float eyeAdjustmentGL[] = Tool.convert2GLMatrix(renderingPrimitives
                    .getEyeDisplayAdjustmentMatrix(viewID)).getData();

            float projectionMatrix[] = new float[16];
            Matrix.multiplyMM(projectionMatrix, 0, rawProjectionMatrixGL, 0, eyeAdjustmentGL, 0);

            currentView = viewID;

            if (currentView != VIEW.VIEW_POSTPROCESS)
                renderingInterface.renderFrame(state, projectionMatrix);
        }

        renderer.end();
    }

    private void setNearFarPlanes(float near, float far) {
        nearPlane = near;
        farPlane = far;
    }

    public void renderVideoBackground() {
        if (currentView == VIEW.VIEW_POSTPROCESS)
            return;

        int vbVideoTextureUnit = 0;
        videoBackgroundTexture.setTextureUnit(vbVideoTextureUnit);
        if (!renderer.updateVideoBackgroundTexture(videoBackgroundTexture)) {
            Log.e(LOGTAG, "Unable to update video background texture");
            return;
        }

        float[] vbProjectionMatrix = Tool
                .convert2GLMatrix(renderingPrimitives
                        .getVideoBackgroundProjectionMatrix(currentView, COORDINATE_SYSTEM_TYPE.COORDINATE_SYSTEM_CAMERA))
                .getData();

        if (Device.getInstance().isViewerActive()) {
            float sceneScaleFactor = (float) getSceneScaleFactor();
            Matrix.scaleM(vbProjectionMatrix, 0, sceneScaleFactor, sceneScaleFactor, 1.0f);
        }

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);

        Mesh vbMesh = renderingPrimitives.getVideoBackgroundMesh(currentView);
        GLES20.glUseProgram(vbShaderProgramID);
        GLES20.glVertexAttribPointer(vbVertexHandle, 3, GLES20.GL_FLOAT, false, 0, vbMesh.getPositions().asFloatBuffer());
        GLES20.glVertexAttribPointer(vbTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0, vbMesh.getUVs().asFloatBuffer());

        GLES20.glUniform1i(vbTexSampler2DHandle, vbVideoTextureUnit);

        GLES20.glEnableVertexAttribArray(vbVertexHandle);
        GLES20.glEnableVertexAttribArray(vbTexCoordHandle);

        GLES20.glUniformMatrix4fv(vbProjectionMatrixHandle, 1, false, vbProjectionMatrix, 0);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, vbMesh.getNumTriangles() * 3, GLES20.GL_UNSIGNED_SHORT,
                vbMesh.getTriangles().asShortBuffer());

        GLES20.glDisableVertexAttribArray(vbVertexHandle);
        GLES20.glDisableVertexAttribArray(vbTexCoordHandle);

        Utils.checkGLError("Rendering of the video background failed");
    }

    private double getSceneScaleFactor() {
        Vec2F fovVector = CameraDevice.getInstance().getCameraCalibration().getFieldOfViewRads();
        float cameraFovYRads = fovVector.getData()[1];

        float virtualFovYRads = VIRTUAL_FOV_Y_DEGS * M_PI / 180;

        return Math.tan(cameraFovYRads / 2) / Math.tan(virtualFovYRads / 2);
    }

    public void configureVideoBackground() {
        CameraDevice cameraDevice = CameraDevice.getInstance();
        VideoMode videoMode = cameraDevice.getVideoMode(CameraDevice.MODE.MODE_DEFAULT);

        VideoBackgroundConfig config = new VideoBackgroundConfig();
        config.setEnabled(true);
        config.setPosition(new Vec2I(0, 0));

        int xSize = 0, ySize = 0;
        if (isPortrait) {
            xSize = (int) (videoMode.getHeight() * (screenHeight / (float) videoMode
                    .getWidth()));
            ySize = screenHeight;

            if (xSize < screenWidth) {
                xSize = screenWidth;
                ySize = (int) (screenWidth * (videoMode.getWidth() / (float) videoMode
                        .getHeight()));
            }
        } else {
            xSize = screenWidth;
            ySize = (int) (videoMode.getHeight() * (screenWidth / (float) videoMode
                    .getWidth()));

            if (ySize < screenHeight) {
                xSize = (int) (screenHeight * (videoMode.getWidth() / (float) videoMode
                        .getHeight()));
                ySize = screenHeight;
            }
        }

        config.setSize(new Vec2I(xSize, ySize));

        Log.i(LOGTAG, "Configure Video Background : Video (" + videoMode.getWidth()
                + " , " + videoMode.getHeight() + "), Screen (" + screenWidth + " , "
                + screenHeight + "), mSize (" + xSize + " , " + ySize + ")");

        Renderer.getInstance().setVideoBackgroundConfig(config);
    }

    private void storeScreenDimensions() {
        Point size = new Point();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            activity.getWindowManager().getDefaultDisplay().getRealSize(size);
        }
        screenWidth = size.x;
        screenHeight = size.y;
    }

    private void updateActivityOrientation() {
        Configuration config = activity.getResources().getConfiguration();

        switch (config.orientation) {
            case Configuration.ORIENTATION_PORTRAIT:
                isPortrait = true;
                break;
            case Configuration.ORIENTATION_LANDSCAPE:
                isPortrait = false;
                break;
            case Configuration.ORIENTATION_UNDEFINED:
            default:
                break;
        }

        Log.i(LOGTAG, "Activity is in "
                + (isPortrait ? "PORTRAIT" : "LANDSCAPE"));
    }
}
