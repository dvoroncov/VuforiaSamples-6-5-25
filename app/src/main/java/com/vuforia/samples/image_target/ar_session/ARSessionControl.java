package com.vuforia.samples.image_target.ar_session;

import com.vuforia.State;
import com.vuforia.samples.image_target.utils.ARException;

public interface ARSessionControl {

    boolean doInitTrackers();

    boolean doLoadTrackersData();

    void doStartTrackers();

    void doStopTrackers();

    boolean doUnloadTrackersData();

    boolean doDeinitTrackers();

    void onInitARDone(ARException e);

    void onVuforiaUpdate(State state);

    void onVuforiaResumed();

    void onVuforiaStarted();
}
