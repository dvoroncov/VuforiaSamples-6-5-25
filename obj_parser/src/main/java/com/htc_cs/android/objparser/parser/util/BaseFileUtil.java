package com.htc_cs.android.objparser.parser.util;

import android.graphics.Bitmap;

import java.io.BufferedReader;

public abstract class BaseFileUtil {
	
	protected String baseFolder = null;

	/* (non-Javadoc)
	 * @see edu.dhbw.andobjviewer.util.FileUtilInterface#getBaseFolder()
	 */
	public String getBaseFolder() {
		return baseFolder;
	}

	/* (non-Javadoc)
	 * @see edu.dhbw.andobjviewer.util.FileUtilInterface#setBaseFolder(java.io.File)
	 */
	public void setBaseFolder(String baseFolder) {
		this.baseFolder = baseFolder;
	}

	/**
	 * get an reader through it's filename
	 * @param name
	 * @return may be null, in case of an exception 
	 */
	public abstract BufferedReader getReaderFromName(String name);

	/**
	 * get a bitmap object through an filename.
	 * @param name
	 * @return may be null, in case of an exception 
	 */
	public abstract Bitmap getBitmapFromName(String name);

}