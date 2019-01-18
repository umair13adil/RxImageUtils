package com.blackbox.imageutils.utils

import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import javax.crypto.*
import javax.crypto.spec.SecretKeySpec

object ImageCrypter {

    private val TAG = "ImageCrypter"

    //Algorithm
    private const val ALGORITHM = "AES"

    //encryption variables
    private var key: SecretKey? = null

    // 128-Bit Key
    private var salt = "A8768CC5BEAA6093"

    //Image Name
    const val TEMP_IMAGE_TAG = "temp_"

    init {
        // Get key
        key = getKey()
    }

    /**
     * This will load decrypted image into ImageView using Glide.
     */
    fun loadImage(imageView: ImageView, path: String, context: Context) {

        ImageCrypter.decryptImage(path)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                        onNext = {

                            //Clear previous image
                            GlideApp.with(context).clear(imageView)

                            //Load image
                            GlideApp.with(context)
                                    .load(it)
                                    .apply(
                                            RequestOptions
                                                    .skipMemoryCacheOf(true)
                                                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                                                    .skipMemoryCache(true)
                                    )
                                    .listener(object : RequestListener<Drawable> {
                                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                                            e?.printStackTrace()
                                            return false
                                        }

                                        override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                                            deleteFile(it.path)
                                            return false
                                        }
                                    })
                                    .into(imageView)

                        },
                        onError = {
                            it.printStackTrace()
                        }
                )
    }

    /**
     * This will encrypt all images that are provided in list.
     */
    fun encryptImageList(imagesList: ArrayList<String>): Observable<File> {

        val listSize = imagesList.size
        var count = 0

        return Observable.create { emitter ->

            Observable.fromArray(imagesList)
                .flatMapIterable { it -> it }
                .concatMap {
                    encryptImage(it)
                }
                .subscribeBy(
                    onNext = {

                        if (!emitter.isDisposed) {
                            emitter.onNext(it)
                        }

                        //Increase count
                        count++

                        //Check if all items are sent
                        if (count >= listSize) {

                            if (!emitter.isDisposed) {
                                emitter.onComplete()
                            }
                        }
                    },
                    onError = {
                        if (!emitter.isDisposed) {
                            emitter.onError(it)
                        }
                    }
                )
        }
    }

    /**
     * This will encrypt provided image and save it as a copy with appended name 'temp_'
     */
    fun encryptImage(originalFilePath: String?): Observable<File> {

        originalFilePath?.let {

            val encryptedImagePath = createCopyOfOriginalFile(originalFilePath)

            return Observable.create { emitter ->

                try {

                    try {
                        val fis = FileInputStream(originalFilePath)
                        val aes = Cipher.getInstance(ALGORITHM)
                        aes.init(Cipher.ENCRYPT_MODE, key)
                        val fs = FileOutputStream(File(encryptedImagePath))
                        val out = CipherOutputStream(fs, aes)
                        out.write(fis.readBytes())
                        out.flush()
                        out.close()
                    } catch (e: NoSuchAlgorithmException) {
                        e.printStackTrace()

                        if (!emitter.isDisposed) {
                            emitter.onError(e)
                            emitter.onComplete()
                        }
                    } catch (e: NoSuchPaddingException) {
                        e.printStackTrace()

                        if (!emitter.isDisposed) {
                            emitter.onError(e)
                            emitter.onComplete()
                        }
                    } catch (e: InvalidKeyException) {
                        e.printStackTrace()

                        if (!emitter.isDisposed) {
                            emitter.onError(e)
                            emitter.onComplete()
                        }
                    } catch (e: IllegalBlockSizeException) {
                        e.printStackTrace()

                        if (!emitter.isDisposed) {
                            emitter.onError(e)
                            emitter.onComplete()
                        }
                    } catch (e: BadPaddingException) {
                        e.printStackTrace()

                        if (!emitter.isDisposed) {
                            emitter.onError(e)
                            emitter.onComplete()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()

                        if (!emitter.isDisposed) {
                            emitter.onError(e)
                            emitter.onComplete()
                        }
                    }

                    //Delete original file
                    deleteFile(originalFilePath)

                    //Rename encrypted image file to original name
                    val createdFile = renameImageToOriginalFileName(encryptedImagePath)

                    if (!emitter.isDisposed) {
                        emitter.onNext(File(createdFile))
                        emitter.onComplete()
                    }

                } catch (e: Exception) {
                    e.printStackTrace()

                    if (!emitter.isDisposed) {
                        emitter.onError(e)
                        emitter.onComplete()
                    }
                }
            }
        }
    }

    /**
     * This will decrypt provided image path and return image file path with appended name 'temp_'
     */
    fun decryptImage(originalFilePath: String?): Observable<File> {

        originalFilePath?.let {

            val decryptedFilePath = createCopyOfOriginalFile(originalFilePath)

            return Observable.create { emitter ->

                try {

                    val fis = FileInputStream(originalFilePath)

                    try {
                        val aes = Cipher.getInstance(ALGORITHM)
                        aes.init(Cipher.DECRYPT_MODE, key)
                        val out = CipherInputStream(fis, aes)

                        File(decryptedFilePath).outputStream().use {
                            out.copyTo(it)
                        }

                    } catch (ex: NoSuchAlgorithmException) {
                        ex.printStackTrace()

                        if (!emitter.isDisposed) {
                            emitter.onError(ex)
                            emitter.onComplete()
                        }
                    } catch (ex: NoSuchPaddingException) {
                        ex.printStackTrace()

                        if (!emitter.isDisposed) {
                            emitter.onError(ex)
                            emitter.onComplete()
                        }
                    } catch (ex: InvalidKeyException) {
                        ex.printStackTrace()

                        if (!emitter.isDisposed) {
                            emitter.onError(ex)
                            emitter.onComplete()
                        }
                    } catch (ex: IOException) {
                        ex.printStackTrace()

                        if (!emitter.isDisposed) {
                            emitter.onError(ex)
                            emitter.onComplete()
                        }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()

                    if (!emitter.isDisposed) {
                        emitter.onError(e)
                        emitter.onComplete()
                    }
                }

                if (!emitter.isDisposed) {
                    emitter.onNext(File(decryptedFilePath))
                    emitter.onComplete()
                }
            }
        }
    }

    /**
     * This will decrypt list of encrypted files provided and return list of decrypted files.
     */
    fun decryptFiles(lisOfEncryptedFiles: ArrayList<String>): Observable<List<File>> {

        return Observable.create { emitter ->

            val listOfDecryptedFiles = arrayListOf<File>()

            Observable.just(lisOfEncryptedFiles)
                    .flatMapIterable { it -> it }
                    .flatMap {
                        decryptImage(it)
                    }
                    .subscribeBy(
                            onNext = {
                                listOfDecryptedFiles.add(it)
                            },
                            onError = {
                                it.printStackTrace()

                                if (!emitter.isDisposed) {
                                    emitter.onError(it)
                                    emitter.onComplete()
                                }
                            },
                            onComplete = {

                                if (!emitter.isDisposed) {
                                    emitter.onNext(listOfDecryptedFiles)
                                    emitter.onComplete()
                                }
                            }
                    )
        }
    }

    private fun renameImageToOriginalFileName(path: String): String {
        val filePath = ImageCrypter.getImageParentPath(path)
        val imageName = ImageCrypter.getImageNameFromPath(path)

        val from = File(filePath, imageName)

        val renameTo = imageName!!.replace(TEMP_IMAGE_TAG, "")

        val to = File(filePath, renameTo)
        if (from.exists())
            from.renameTo(to)

        return to.path
    }

    private fun createCopyOfOriginalFile(originalFilePath: String): String {

        val filePath = ImageCrypter.getImageParentPath(originalFilePath)
        val imageName = ImageCrypter.getImageNameFromPath(originalFilePath)

        val originalFile = File(originalFilePath)
        val copyFile = File(filePath, "$TEMP_IMAGE_TAG$imageName")

        //Create a copy of original file
        try {
            FileUtils.copy(originalFile, copyFile)
        }catch(ex : IOException){
            ex.printStackTrace()
        }

        return copyFile.path
    }

    private fun deleteFile(path: String) {
        val file = File(path)

        if (file.exists())
            file.delete()
    }

    private fun getImageParentPath(path: String?): String? {
        var newPath = ""
        path?.let {
            newPath = it.substring(0, it.lastIndexOf("/") + 1)
        }
        return newPath
    }

    private fun getImageNameFromPath(path: String?): String? {
        var newPath = ""
        path?.let {
            newPath = it.substring(it.lastIndexOf("/") + 1)
        }
        return newPath
    }

    private fun getKey(): SecretKey? {

        var secretKey: SecretKey? = null

        try {
            secretKey = SecretKeySpec(salt.toBytes(), ALGORITHM)
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }

        return secretKey
    }

    /*
     * This will convert string to byte array.
     */
    private fun String.toBytes(): ByteArray {
        return this.toByteArray(Charsets.UTF_8)
    }
}
