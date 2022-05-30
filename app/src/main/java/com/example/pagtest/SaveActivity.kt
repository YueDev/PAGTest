package com.example.pagtest

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.MenuItem
import android.view.View
import androidx.annotation.RequiresApi
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import com.example.pagtest.databinding.ActivitySaveBinding
import org.libpag.PAGFile
import java.io.File
import java.io.FileDescriptor
import java.util.*

class SaveActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySaveBinding
    private var recorder: PagRecorder? = null
    private lateinit var pagFile: PAGFile

    private var canBack = false

    companion object {

        var sPagFile: PAGFile? = null

        fun startNewInstance(context: Context, pagFile: PAGFile, musicUri: Uri?) {
            sPagFile = pagFile
            val intent = Intent(context, SaveActivity::class.java)
            musicUri?.let {
                intent.data = it
            }
            context.startActivity(intent)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pagFile = sPagFile ?: return
        sPagFile = null

        val musicUri = intent.data

        supportActionBar?.let {
            it.title = "保存"
            it.setDisplayHomeAsUpEnabled(true)
            it.elevation = 0f
        }

        binding = ActivitySaveBinding.inflate(layoutInflater)
        setContentView(binding.root)

        recorder = PagRecorder(pagFile)

        val fileName = "PAG-${UUID.randomUUID()}"

        if (PagRecorder.isHighVersion()) {
            Thread { saveVideoHighVersion(fileName, musicUri) }.start()
        } else {
            Thread { saveVideo(fileName, musicUri) }.start()
        }

    }

    //低版本保存
    @WorkerThread
    private fun saveVideo(fileName: String, musicUri: Uri?) {
        val outDir = File("${Environment.getExternalStorageDirectory().path}${File.separator}PAG Test")
        if (!outDir.exists()) {
            outDir.mkdirs()
        }

        val outPath = "$outDir${File.separator}${fileName}.mp4"

        val musicContext = if (musicUri != null) this else null

        recorder?.record(outPath, null, null, null, musicUri, musicContext, object : PagRecorder.Listener {
            override fun onSuccess() {
                runOnUiThread { onSuccessUI() }
                PagRecorder.insertVideoToMediaStore(
                    this@SaveActivity,
                    outPath,
                    System.currentTimeMillis(),
                    sPagFile?.width() ?: 0,
                    sPagFile?.height() ?: 0,
                    sPagFile?.duration() ?: 0,
                    null
                )
            }

            override fun onProgress(progress: Float) {
                runOnUiThread { onProgressUI(progress) }
            }

            override fun onError(e: java.lang.Exception?) {
                runOnUiThread { onErrorUI(e) }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        recorder?.releaseEncoder()
        sPagFile = null
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }
        else -> false
    }


    //29及其以上保存
    @WorkerThread
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveVideoHighVersion(fileName: String, musicUri: Uri?) {

        val values = ContentValues()
        values.put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
        values.put(MediaStore.Video.Media.TITLE, fileName)
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        values.put(MediaStore.Video.Media.DESCRIPTION, fileName)
        values.put(MediaStore.Video.Media.IS_PENDING, 1)

        val insertUri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: let {
            runOnUiThread {
                onErrorUI(Exception("save error: insertUri == null"))
            }
            return
        }

        contentResolver.openFileDescriptor(insertUri, "w")?.use {

            val outFd: FileDescriptor = it.fileDescriptor

            val musicContext = if (musicUri != null) this else null

            recorder?.record(null, outFd, null, null, musicUri, musicContext, object : PagRecorder.Listener {
                override fun onSuccess() {
                    values.clear()
                    values.put(MediaStore.Video.Media.IS_PENDING, 0)
                    contentResolver.update(insertUri, values, null, null)
                    runOnUiThread { onSuccessUI() }
                }

                override fun onProgress(progress: Float) {
                    runOnUiThread { onProgressUI(progress) }
                }

                override fun onError(e: java.lang.Exception?) {
                    runOnUiThread { onErrorUI(e) }
                }

            })

        }
    }


    @UiThread
    fun onSuccessUI() {
        binding.textView.text = "Done"
        binding.loadingBar.visibility = View.INVISIBLE
        canBack = true
    }

    @UiThread
    fun onProgressUI(progress: Float) {
        binding.progressBar.progress = progress.toInt()
        binding.progressTextView.text = progress.toInt().toString()
    }

    @UiThread
    fun onErrorUI(e: java.lang.Exception?) {
        binding.textView.text = "Error!\n ${e?.message}"
        canBack = true
    }


    override fun onBackPressed() {
        if (!canBack) return
        super.onBackPressed()
    }

}



