package com.blackbox.imageutils

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.View
import android.widget.Toast
import com.blackbox.imageutils.adapters.ImageAdapter
import com.blackbox.imageutils.models.ImageItem
import com.blackbox.imageutils.utils.Constants
import com.blackbox.imageutils.utils.FileUtils
import com.blackbox.imageutils.utils.ImageCrypter
import com.blackbox.imageutils.utils.PermissionsHelper
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: ImageAdapter
    private lateinit var layoutManager: StaggeredGridLayoutManager
    private val listOfEncryptedFilePaths = arrayListOf<String>()
    private val listOfDecryptedFilePaths = arrayListOf<String>()
    private val listOfImages = arrayListOf<ImageItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Check Permissions
        checkStoragePermissions()

        //Setup List
        setUpListAdapter()

        btn_decrypt.setOnClickListener {

            if (listOfEncryptedFilePaths.isNotEmpty()) {

                showHideProgress(true)

                decryptImages()

                btn_decrypt.visibility = View.GONE

            } else {
                Toast.makeText(this, "There are no encrypted images!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * This will encrypt images and overwrite original files.
     */
    private fun encryptImages() {

        showHideProgress(true)

        val directory = File(Constants.imagesPath)
        val listOfPaths = arrayListOf<String>()

        if (directory.exists()) {

            //Get list of files in storage directory
            val list = directory.listFiles()

            for (item in list) {
                listOfPaths.add(item.path)
            }

            //Encrypt all Images
            ImageCrypter.encryptImageList(listOfPaths)
                .delay(1,TimeUnit.SECONDS)
                .subscribeBy(
                    onNext = {
                        listOfEncryptedFilePaths.add(it.path)
                    },
                    onError = {
                        it.printStackTrace()
                        showHideProgress(false)
                        Toast.makeText(this, "Something went wrong.", Toast.LENGTH_SHORT).show()
                    },
                    onComplete = {
                        runOnUiThread {
                            Toast.makeText(this, "All image files are encrypted!", Toast.LENGTH_SHORT).show()
                            btn_decrypt.visibility = View.VISIBLE
                            showHideProgress(false)
                        }
                    }
                )
        }
    }

    /**
     * This will decrypt images.
     */
    private fun decryptImages(){

        ImageCrypter.decryptFiles(listOfEncryptedFilePaths)
            .subscribeBy(
                onNext = {
                    it.forEach {
                        listOfDecryptedFilePaths.add(it.path)
                    }
                },
                onError = {
                    it.printStackTrace()
                    showHideProgress(false)
                    Toast.makeText(this, "Something went wrong.", Toast.LENGTH_SHORT).show()
                    btn_decrypt.visibility = View.VISIBLE
                },
                onComplete = {
                    showHideProgress(false)

                    //Get list of decrypted files
                    for (item in listOfDecryptedFilePaths) {
                        listOfImages.add(ImageItem(item))
                    }

                    //Send list to RecyclerView
                    adapter.submitList(listOfImages)
                }
            )
    }

    /*
     * Setup RecyclerView list adapter.
     */
    private fun setUpListAdapter() {
        adapter = ImageAdapter()

        layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        layoutManager.gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_NONE

        recycler_view.layoutManager = layoutManager
        recycler_view.adapter = adapter
    }

    private fun showHideProgress(show: Boolean) {
        if (show) {
            progressDialog?.visibility = View.VISIBLE
        } else {
            progressDialog?.visibility = View.GONE
        }
    }

    private fun checkStoragePermissions() {
        //Request for storage permissions then start camera
        PermissionsHelper.requestStoragePermissions(this)
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(AndroidSchedulers.mainThread())
            .debounce(1, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { callback ->

                    FileUtils.createDirIfNotExists(Constants.appStoragePath)
                    FileUtils.createDirIfNotExists(Constants.imagesPath)

                    //Copy Images from assets to storage directory
                    FileUtils.copyAssets(this)

                    //Get list of Images
                    encryptImages()
                },
                onError = {

                }
            )
    }

    override fun onDestroy() {
        super.onDestroy()

        //Delete existing images
        FileUtils.deleteDir(File(Constants.imagesPath))
    }
}
