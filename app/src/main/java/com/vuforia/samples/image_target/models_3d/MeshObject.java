package com.vuforia.samples.image_target.models_3d;

import java.nio.Buffer;

public abstract class MeshObject {

    public enum BUFFER_TYPE {
        BUFFER_TYPE_VERTEX, BUFFER_TYPE_TEXTURE_COORD, BUFFER_TYPE_NORMALS
    }

    public Buffer getVertices() {
        return getBuffer(BUFFER_TYPE.BUFFER_TYPE_VERTEX);
    }

    public Buffer getTexCoords() {
        return getBuffer(BUFFER_TYPE.BUFFER_TYPE_TEXTURE_COORD);
    }

    public Buffer getNormals() {
        return getBuffer(BUFFER_TYPE.BUFFER_TYPE_NORMALS);
    }

    public abstract Buffer getBuffer(BUFFER_TYPE bufferType);

    public abstract int getNumObjectVertex();

    public abstract int getNumObjectIndex();
}
