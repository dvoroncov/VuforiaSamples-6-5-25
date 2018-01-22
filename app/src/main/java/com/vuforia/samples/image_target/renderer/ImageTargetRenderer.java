package com.vuforia.samples.image_target.renderer;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.widget.SeekBar;

import com.vuforia.Device;
import com.vuforia.Matrix44F;
import com.vuforia.State;
import com.vuforia.Tool;
import com.vuforia.Trackable;
import com.vuforia.TrackableResult;
import com.vuforia.Vuforia;
import com.vuforia.samples.image_target.R;
import com.vuforia.samples.image_target.ar_session.ARSession;
import com.vuforia.samples.image_target.models_3d.Amenemhat;
import com.vuforia.samples.image_target.models_3d.MeshObject;
import com.vuforia.samples.image_target.renderer.shaders.Shaders;
import com.vuforia.samples.image_target.ui.ar.ImageTargetsActivity;
import com.vuforia.samples.image_target.utils.LoadingDialogHandler;
import com.vuforia.samples.image_target.utils.Texture;
import com.vuforia.samples.image_target.utils.Utils;

import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class ImageTargetRenderer
        implements GLSurfaceView.Renderer, ARRendererControl, SeekBar.OnSeekBarChangeListener {

    private static final String LOGTAG = "ImageTargetRenderer";

    private ARSession vuforiaAppSession;
    private ImageTargetsActivity activity;
    private ARRenderer mARRenderer;

    private Vector<Texture> mTextures;

    private int shaderProgramID;
    private int vertexHandle;
    private int textureCoordHandle;
    private int vertexNormalHandle;
    private int mvpMatrixHandle;
    private int texSampler2DHandle;
    private int cameraPositionHandle;
    private int lightPositionHandle;
    private int vertexPositionOffsetHandle;

    private float lightPositionX = 0;
    private float lightPositionY = 0;
    private float lightPositionZ = 0;

    private float vertexPositionOffsetX = 0;
    private float vertexPositionOffsetY = 0;
    private float vertexPositionOffsetZ = 0;

    private MeshObject meshObject;

    private boolean mIsActive = false;
    private boolean modelIsLoaded = false;

    private static final float OBJECT_SCALE_FLOAT = 0.003f;


    public ImageTargetRenderer(ImageTargetsActivity activity, ARSession session) {
        this.activity = activity;
        vuforiaAppSession = session;
        // ARRenderer used to encapsulate the use of RenderingPrimitives setting
        // the device mode AR/VR and stereo mode
        mARRenderer = new ARRenderer(this, this.activity, Device.MODE.MODE_AR, false, 0.01f, 5f);
    }


    // Called to draw the current frame.
    @Override
    public void onDrawFrame(GL10 gl) {
        if (!mIsActive)
            return;

        // Call our function to render content from ARRenderer class
        mARRenderer.render();
    }


    public void setActive(boolean active) {
        mIsActive = active;

        if (mIsActive)
            mARRenderer.configureVideoBackground();
    }


    // Called when the surface is created or recreated.
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d(LOGTAG, "GLRenderer.onSurfaceCreated");

        // Call Vuforia function to (re)initialize rendering after first use
        // or after OpenGL ES context was lost (e.g. after onPause/onResume):
        vuforiaAppSession.onSurfaceCreated();

        mARRenderer.onSurfaceCreated();
    }


    // Called when the surface changed size.
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d(LOGTAG, "GLRenderer.onSurfaceChanged");

        // Call Vuforia function to handle render surface size changes:
        vuforiaAppSession.onSurfaceChanged(width, height);

        // RenderingPrimitives to be updated when some rendering change is done
        mARRenderer.onConfigurationChanged(mIsActive);

        initRendering();
    }


    // Function for initializing the renderer.
    private void initRendering() {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, Vuforia.requiresAlpha() ? 0.0f
                : 1.0f);

        for (Texture t : mTextures) {
            GLES20.glGenTextures(1, t.textureID, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t.textureID[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                    t.width, t.height, 0, GLES20.GL_RGBA,
                    GLES20.GL_UNSIGNED_BYTE, t.data);
        }

        shaderProgramID = Utils.createProgramFromShaderSrc(
                Shaders.CUBE_MESH_VERTEX_SHADER,
                Shaders.CUBE_MESH_FRAGMENT_SHADER);

        vertexHandle = GLES20.glGetAttribLocation(shaderProgramID,
                "vertexPosition");
        textureCoordHandle = GLES20.glGetAttribLocation(shaderProgramID,
                "vertexTexCoord");
        vertexNormalHandle = GLES20.glGetAttribLocation(shaderProgramID,
                "vertexNormal");
        mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgramID,
                "modelViewProjectionMatrix");
        texSampler2DHandle = GLES20.glGetUniformLocation(shaderProgramID,
                "texSampler2D");
        cameraPositionHandle = GLES20.glGetUniformLocation(shaderProgramID,
                "cameraPosition");
        lightPositionHandle = GLES20.glGetUniformLocation(shaderProgramID,
                "lightPosition");
        vertexPositionOffsetHandle = GLES20.glGetUniformLocation(shaderProgramID,
                "vertexPositionOffset");

        if (!modelIsLoaded) {
            meshObject = new Amenemhat(activity);

            activity.getLoadingDialogHandler()
                    .sendEmptyMessage(LoadingDialogHandler.HIDE_LOADING_DIALOG);
        }

    }

    public void updateConfiguration() {
        mARRenderer.onConfigurationChanged(mIsActive);
    }

    // The render function called from SampleAppRendering by using RenderingPrimitives views.
    // The state is owned by ARRenderer which is controlling it's lifecycle.
    // State should not be cached outside this method.
    public void renderFrame(State state, float[] projectionMatrix) {
        // Renders video background replacing Renderer.DrawVideoBackground()
        mARRenderer.renderVideoBackground();

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // handle face culling, we need to detect if we are using reflection
        // to determine the direction of the culling
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);

        // Did we find any trackables this frame?
        for (int tIdx = 0; tIdx < state.getNumTrackableResults(); tIdx++) {
            TrackableResult result = state.getTrackableResult(tIdx);
            Trackable trackable = result.getTrackable();
            printUserData(trackable);
            Matrix44F modelViewMatrix_Vuforia = Tool
                    .convertPose2GLMatrix(result.getPose());
            float[] modelViewMatrix = modelViewMatrix_Vuforia.getData();

            // deal with the modelview and projection matrices
            float[] modelViewProjection = new float[16];

            Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f,
                    OBJECT_SCALE_FLOAT);
            Matrix.scaleM(modelViewMatrix, 0, OBJECT_SCALE_FLOAT,
                    OBJECT_SCALE_FLOAT, OBJECT_SCALE_FLOAT);

            Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelViewMatrix, 0);

            // activate the shader program and bind the vertex/normal/tex coords
            GLES20.glUseProgram(shaderProgramID);

            for (int i = 0; i < meshObject.getGroupCount(); i++) {
                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                        false, 0, meshObject.getVerticesBuffer(i));
                GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT,
                        false, 0, meshObject.getTextureBuffer(i));
                GLES20.glVertexAttribPointer(vertexNormalHandle, 3, GLES20.GL_FLOAT,
                        false, 0, meshObject.getNormalsBuffer(i));

                GLES20.glEnableVertexAttribArray(vertexHandle);
                GLES20.glEnableVertexAttribArray(textureCoordHandle);
                GLES20.glEnableVertexAttribArray(vertexNormalHandle);

                // activate texture 0, bind it, and pass to shader
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                        mTextures.get(0).textureID[0]);
                GLES20.glUniform1i(texSampler2DHandle, 0);

                // pass the model view matrix to the shader
                GLES20.glUniformMatrix4fv(cameraPositionHandle, 1, false,
                        modelViewMatrix, 0);
                GLES20.glUniform3f(lightPositionHandle, lightPositionX, lightPositionY, lightPositionZ);
                GLES20.glUniform3f(vertexPositionOffsetHandle, vertexPositionOffsetX, vertexPositionOffsetY, vertexPositionOffsetZ);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                        modelViewProjection, 0);

                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, meshObject.getVertexCount(i));

                // disable the enabled arrays
                GLES20.glDisableVertexAttribArray(vertexHandle);
                GLES20.glDisableVertexAttribArray(textureCoordHandle);

                Utils.checkGLError("Render Frame");
            }
        }

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

    }

    private void printUserData(Trackable trackable) {
        String userData = (String) trackable.getUserData();
        Log.d(LOGTAG, "UserData:Retreived User Data	\"" + userData + "\"");
    }


    public void setTextures(Vector<Texture> textures) {
        mTextures = textures;

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            switch (seekBar.getId()) {
                case R.id.x_light_seek_bar:
                    lightPositionX = progress - (seekBar.getMax() / 2);
                    break;
                case R.id.y_light_seek_bar:
                    lightPositionY = progress - (seekBar.getMax() / 2);
                    break;
                case R.id.z_light_seek_bar:
                    lightPositionZ = progress - (seekBar.getMax() / 2);
                    break;
                case R.id.x_offset_seek_bar:
                    vertexPositionOffsetX = progress - (seekBar.getMax() / 2);
                    break;
                case R.id.y_offset_seek_bar:
                    vertexPositionOffsetY = progress - (seekBar.getMax() / 2);
                    break;
                case R.id.z_offset_seek_bar:
                    vertexPositionOffsetZ = progress - (seekBar.getMax() / 2);
                    break;
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
