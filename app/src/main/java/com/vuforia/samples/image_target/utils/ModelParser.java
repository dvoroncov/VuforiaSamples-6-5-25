package com.vuforia.samples.image_target.utils;

import android.content.Context;

import com.htc_cs.android.objparser.parser.models.Model;
import com.htc_cs.android.objparser.parser.parser.ObjParser;
import com.htc_cs.android.objparser.parser.parser.ParseException;
import com.htc_cs.android.objparser.parser.util.AssetsFileUtil;
import com.htc_cs.android.objparser.parser.util.BaseFileUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ModelParser {

    private Model model;

    public ModelParser(Context context, String fileName) {
        BaseFileUtil fileUtil = new AssetsFileUtil(context.getAssets());
        fileUtil.setBaseFolder("");
        BufferedReader fileReader = fileUtil.getReaderFromName(fileName);
        ObjParser objParser = new ObjParser(fileUtil);
        if(fileReader != null) {
            try {
                model = objParser.parse("amenemhat", fileReader);
                model.finalize();
            } catch (IOException | ParseException e) {
                e.printStackTrace();
            }
        }
    }

    public int getIndicesNumber() {
        return model.getGroups().get(0).vertexCount;
    }

    public int getVerticesNumber() {
        return model.getGroups().get(0).vertexCount / 3;
    }

    public Buffer getTextureBuffer() {
        return model.getGroups().get(0).texcoords.position(0);
    }

    public Buffer getVerticesBuffer() {
        return model.getGroups().get(0).vertices.position(0);
    }

    public Buffer getNormalsBuffer() {
        return model.getGroups().get(0).normals.position(0);
    }
}
