package com.vuforia.samples.image_target.models_3d;

import android.content.Context;

import com.htc_cs.android.objparser.parser.models.Group;
import com.htc_cs.android.objparser.parser.models.Material;
import com.vuforia.samples.image_target.utils.ModelParser;

import java.nio.Buffer;
import java.util.ArrayList;
import java.util.List;

public class Amenemhat extends MeshObject {

    private static final String OBJ_FILE_NAME = "Shotgun.obj";

    private List<Group> groups = new ArrayList<>();

    public Amenemhat(Context context) {
        ModelParser modelParser = new ModelParser(context, OBJ_FILE_NAME);
        groups = modelParser.getGroups();
    }

    @Override
    public int getGroupCount() {
        return groups.size();
    }

    @Override
    public int getVertexCount(int index) {
        return groups.get(index).vertexCount;
    }

    @Override
    public Buffer getTextureBuffer(int index) {
        return groups.get(index).texcoords.position(0);
    }

    @Override
    public Buffer getVerticesBuffer(int index) {
        return groups.get(index).vertices.position(0);
    }

    @Override
    public Buffer getNormalsBuffer(int index) {
        return groups.get(index).normals.position(0);
    }

    @Override
    public Material getMaterial(int index) {
        return groups.get(index).getMaterial();
    }
}
