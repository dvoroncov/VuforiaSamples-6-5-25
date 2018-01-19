package com.vuforia.samples.image_target.renderer;

import com.vuforia.State;

public interface ARRendererControl {

    void renderFrame(State state, float[] projectionMatrix);
}
