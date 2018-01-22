package com.vuforia.samples.image_target.models_3d;

import com.htc_cs.android.objparser.parser.models.Material;

import java.nio.Buffer;

public abstract class MeshObject {

    public abstract int getGroupCount();

    public abstract Buffer getTextureBuffer(int index);

    public abstract Buffer getVerticesBuffer(int index);

    public abstract Buffer getNormalsBuffer(int index);

    public abstract Material getMaterial(int index);

    public abstract int getVertexCount(int index);
}
