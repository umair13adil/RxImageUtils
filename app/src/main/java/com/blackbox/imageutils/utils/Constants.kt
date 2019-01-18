package com.blackbox.imageutils.utils

import android.os.Environment
import java.io.File

/**
 * Created by umair on 12/07/2017.
 */
class Constants {

    companion object {

        //AppFolder
        private val appFolder = "ImageUtils Library"
        private val imagesFolder = "Images"

        //Images
        val appStoragePath = (Environment.getExternalStorageDirectory().toString() + File.separator + appFolder
                + File.separator)
        val imagesPath = appStoragePath + imagesFolder + File.separator
    }
}