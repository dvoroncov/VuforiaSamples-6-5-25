package com.vuforia.samples.image_target.models_3d;

import android.content.Context;

import com.vuforia.samples.image_target.utils.ModelParser;

import java.nio.Buffer;

public class Amenemhat extends MeshObject {

    private static final String OBJ_FILE_NAME = "3dModels/amenemhat.obj";

    private ModelParser modelParser;
    private Buffer vertBuff;
    private Buffer texCoordBuff;
    private Buffer normBuff;

    private int indicesNumber = 0;
    private int verticesNumber = 0;


    public Amenemhat(Context context) {
        modelParser = new ModelParser(context, OBJ_FILE_NAME);

        vertBuff = modelParser.getVerticesBuffer();
        texCoordBuff = modelParser.getTextureBuffer();
        normBuff = modelParser.getNormalsBuffer();

        indicesNumber = modelParser.getIndicesNumber();
        verticesNumber = modelParser.getVerticesNumber();
    }

    public int getNumObjectIndex() {
        return indicesNumber;
    }

    @Override
    public int getNumObjectVertex() {
        return verticesNumber;
    }

    @Override
    public Buffer getBuffer(BUFFER_TYPE bufferType) {

        Buffer result = null;
        switch (bufferType) {
            case BUFFER_TYPE_VERTEX:
                result = vertBuff;
                break;
            case BUFFER_TYPE_TEXTURE_COORD:
                result = texCoordBuff;
                break;
            case BUFFER_TYPE_NORMALS:
                result = normBuff;
                break;
            default:
                break;

        }

        return result;
    }
}
