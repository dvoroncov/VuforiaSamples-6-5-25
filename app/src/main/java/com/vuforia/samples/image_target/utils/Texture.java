package com.vuforia.samples.image_target.utils;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Texture {

    private static final String LOGTAG = Texture.class.getSimpleName();

    public int width;
    public int height;
    public ByteBuffer data;
    public int[] textureID = new int[1];
    private int channels;

    public static Texture loadTextureFromApk(String fileName,
                                             AssetManager assets) {
        InputStream inputStream;
        try {
            inputStream = assets.open(fileName, AssetManager.ACCESS_BUFFER);

            BufferedInputStream bufferedStream = new BufferedInputStream(
                    inputStream);
            Bitmap bitMap = BitmapFactory.decodeStream(bufferedStream);

            int[] data = new int[bitMap.getWidth() * bitMap.getHeight()];
            bitMap.getPixels(data, 0, bitMap.getWidth(), 0, 0,
                    bitMap.getWidth(), bitMap.getHeight());

            return loadTextureFromIntBuffer(data, bitMap.getWidth(),
                    bitMap.getHeight());
        } catch (IOException e) {
            Log.e(LOGTAG, "Failed to log texture '" + fileName + "' from APK");
            Log.i(LOGTAG, e.getMessage());
            return null;
        }
    }
    
    private static Texture loadTextureFromIntBuffer(int[] data, int width,
                                                    int height) {
        int numPixels = width * height;
        byte[] dataBytes = new byte[numPixels * 4];

        for (int p = 0; p < numPixels; ++p) {
            int colour = data[p];
            dataBytes[p * 4] = (byte) (colour >>> 16);     // R
            dataBytes[p * 4 + 1] = (byte) (colour >>> 8);  // G
            dataBytes[p * 4 + 2] = (byte) colour;          // B
            dataBytes[p * 4 + 3] = (byte) (colour >>> 24); // A
        }

        Texture texture = new Texture();
        texture.width = width;
        texture.height = height;
        texture.channels = 4;

        texture.data = ByteBuffer.allocateDirect(dataBytes.length).order(
                ByteOrder.nativeOrder());
        int rowSize = texture.width * texture.channels;
        for (int r = 0; r < texture.height; r++)
            texture.data.put(dataBytes, rowSize * (texture.height - 1 - r),
                    rowSize);

        texture.data.rewind();

        return texture;
    }
}
