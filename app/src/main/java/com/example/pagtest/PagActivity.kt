package com.example.pagtest

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.postDelayed
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageView
import com.canhub.cropper.options
import com.example.pagtest.databinding.ActivityPagBinding
import com.sangcomz.fishbun.FishBun
import kotlinx.coroutines.launch
import org.libpag.PAGFile
import org.libpag.PAGImage
import org.libpag.PAGView
import org.libpag.PAGView.PAGViewListener
import java.io.FileInputStream


class PagActivity : AppCompatActivity() {
    private val stateManager = StateManager()

    private lateinit var binding: ActivityPagBinding

    private val processBar by lazy { binding.progressBar }

    private val pagView by lazy { binding.pagView }
    private val playButton by lazy { binding.playButton }
    private val musicButton by lazy { binding.musicButton }
    private val saveButton by lazy { binding.saveButton }
    private val seekBar by lazy { binding.seekBar }
    private lateinit var pagFile: PAGFile

    private var mediaPlayer: MediaPlayer? = null

    private lateinit var adapter: PagAdapter

    private var selectIndex = -1

    private val uris = mutableListOf<Uri>()

    private var canDrop = true

    private var canBack = false

    private var musicUri: Uri? = null

    //换图
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            //新方法要求api33
            val uris = it.data?.getParcelableArrayListExtra<Uri>(FishBun.INTENT_PATH) ?: return@registerForActivityResult
            changePicture(selectIndex, uris[0])
        }
    }

    //裁切图片
    private val cropLauncher = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            val uri = result.uriContent ?: return@registerForActivityResult
            cropPicture(selectIndex, uri)
        } else {
            Toast.makeText(this, "裁切图片错误", Toast.LENGTH_SHORT).show()
        }
    }

    //选择音乐
    private val musicLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) {
        it?.let {
            changeMusic(uri = it)
        }
    }


    companion object {
        private const val KEY_PAG_ACTIVITY_IMAGES = "key_pag_activity_images"
        fun startNewInstance(context: Context, pagFileUri: Uri, imageUris: ArrayList<Uri>) {
            Intent(context, PagActivity::class.java).let {
                it.data = pagFileUri
                it.putParcelableArrayListExtra(KEY_PAG_ACTIVITY_IMAGES, imageUris)
                context.startActivity(it)
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.let {
            it.title = "PAG播放器"
            it.setDisplayHomeAsUpEnabled(true)
            it.elevation = 0f
        }

        //根据uri读取pag文件
        val pagFileUri = intent.data ?: return
        contentResolver.openFileDescriptor(pagFileUri, "r")?.use {
            val byteArray = FileInputStream(it.fileDescriptor).readBytes()
            pagFile = PAGFile.Load(byteArray)
        } ?: return

        val list = intent.getParcelableArrayListExtra<Uri>(KEY_PAG_ACTIVITY_IMAGES) ?: return
        uris.addAll(list)

        binding = ActivityPagBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initView()
        initState()
        changeAllPictures(uris)

    }


    private fun initView() {
        processBar.visibility = View.VISIBLE


        //pag flush监听 主要是更新进度条
        pagView.addPAGFlushListener {
            if (!pagView.isPlaying) return@addPAGFlushListener
            val time = pagView.progress * seekBar.max
            seekBar.value = time.toLong()
        }

        //pag的播放监听
        pagView.addListener(object : PAGViewListener {
            override fun onAnimationStart(view: PAGView) {}
            override fun onAnimationEnd(view: PAGView) {
                if (pagView.progress == 1.0) {
                    //判断动画结束，pag会自动停止，更新ui
                    setPlay(false)
                    seekBar.value = seekBar.max
                    changeMusicProgress(0)
                }
            }

            override fun onAnimationCancel(view: PAGView) {}
            override fun onAnimationRepeat(view: PAGView) {}
        })


        //seekbar
        val duration = pagFile.duration()
        seekBar.max = duration
        seekBar.value = 0L

        //seekbar 与pag的
        seekBar.setOnSeekBarTouchListener(object : PagSeekBar.OnTouchListener {
            override fun onStartTouch(value: Long) {
                if (!canDrop) return
                canDrop = false
                onTouchSeekbar(value)
                pagView.postDelayed(200) { canDrop = true }
            }

            override fun onTouch(value: Long) {
                if (!canDrop) return
                canDrop = false
                onTouchSeekbar(value)
                pagView.postDelayed(200) { canDrop = true }
            }

            override fun onEndTouch(value: Long) {
                onTouchSeekbar(value)
            }
        })


        //button
        musicButton.setOnClickListener {
            setPlay(false)
            musicLauncher.launch("*/*")
        }

        playButton.setOnClickListener {
            val old: Boolean = stateManager.playState.value ?: return@setOnClickListener
            setPlay(!old)
        }


        saveButton.setOnClickListener {
            setPlay(false)
            SaveActivity.startNewInstance(this, pagFile, musicUri)
        }

        //rv
        adapter = PagAdapter(uris, click = {
            clickItem(it)
        }, longClick = {
            longClickItem(it)
        })

        binding.recyclerView.also {
            it.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
            it.adapter = adapter
        }

    }

    private fun longClickItem(position: Int) {
        if (position == 0) {
            pagFile.replaceImage(position, PAGImage.FromBitmap(Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)))
        } else {
            pagFile.replaceImage(position, PAGImage.FromBitmap(null))
        }
        pagView.flush()
        Toast.makeText(this, "已删除pag图片，序号${position}", Toast.LENGTH_SHORT).show()
    }

    private fun onTouchSeekbar(time: Long) {
        setPlay(false)
        changePagProgress(time)

        val musicTime: Long = if (seekBar.progress == 1.0f) 0L else time
        changeMusicProgress(musicTime)
    }


    private fun changeMusicProgress(time: Long) {
        mediaPlayer?.seekTo((time / 1000).toInt())
    }


    //更改pag的进度，microsecond
    private fun changePagProgress(time: Long) {
        pagView.progress = time * 1.0 / seekBar.max
    }


    private fun initState() {
        stateManager.playState.observe(this) { isPlaying ->
            if (isPlaying) {
                playButton.setImageResource(R.drawable.ic_pause)
                pagView.play()
                mediaPlayer?.start()
            } else {
                playButton.setImageResource(R.drawable.ic_play)
                pagView.stop()
                mediaPlayer?.pause()
            }
        }
    }

    private fun setPlay(isPlay: Boolean) {
        val nowPlay: Boolean? = stateManager.playState.value
        if (nowPlay != null && !nowPlay && !isPlay) return
        stateManager.setPlay(isPlay)
    }

    override fun onPause() {
        super.onPause()
        setPlay(false)
    }

    override fun onResume() {
        super.onResume()
        pagView.composition = pagFile
        changePagProgress(seekBar.value)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.let {
            it.stop()
            it.release()
        }
    }

    //替换所有图片
    private fun changeAllPictures(uriList: List<Uri>) {
        lifecycleScope.launch {
            processBar.visibility = View.VISIBLE

            for (i in uriList.indices) {
                pagFile.replaceImage(i, PAGImage.FromBitmap(getBitmapFromUri(uriList[i])))
            }

            pagView.composition = pagFile
            pagView.flush()
            processBar.visibility = View.GONE
            canBack = true
        }
    }

    //点击rv的item
    private fun clickItem(i: Int) {
        setPlay(false)
        selectIndex = i
        toGallery(this, 1, galleryLauncher)
    }


    //换图 去裁切
    private fun changePicture(index: Int, uri: Uri) {
        adapter.setUri(index, uri)
        cropLauncher.launch(
            options(uri) {
                setGuidelines(CropImageView.Guidelines.ON)
            })
    }


    //裁切
    private fun cropPicture(index: Int, uri: Uri) {
        lifecycleScope.launch {
            val bitmap = getBitmapFromUri(uri)
            pagFile.replaceImage(index, PAGImage.FromBitmap(bitmap))
            pagView.flush()
        }
    }


    private fun changeMusic(uri: Uri) {
        lifecycleScope.launch {
            processBar.visibility = View.VISIBLE
            val fileName = getFileNameFromUri(uri)
            if (fileName.endsWith("m4a")) {
                loadMusic(uri)
            } else {
                Toast.makeText(this@PagActivity, "请选择aac格式的m4a文件", Toast.LENGTH_SHORT).show()
            }
            processBar.visibility = View.GONE
        }
    }

    //加载音乐
    private fun loadMusic(uri: Uri) {

        mediaPlayer?.also {
            it.stop()
            it.reset()
            it.setDataSource(this, uri)
            it.prepare()
        } ?: run {
            mediaPlayer = MediaPlayer().also {
                val attributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                    .build()
                it.setAudioAttributes(attributes)
                it.setDataSource(this, uri)
                it.prepare()
            }
        }

        //重置进度
        changePagProgress(0)
        seekBar.value = 0
        musicUri = uri
    }

    override fun onBackPressed() {
        if (!canBack) return
        super.onBackPressed()
    }


    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }
        100 -> {
            //清除所有图片
            uris.forEachIndexed { index, _ ->
                pagFile.replaceImage(index, PAGImage.FromBitmap(null))
            }
            pagView.flush()
            Toast.makeText(this, "已清除所有图片", Toast.LENGTH_SHORT).show()
            true
        }
        else -> false

    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menu?.add(0, 100, 0, "清除所有图片")
        return true
    }


}


//状态管理 比较简单 没有用ViewModel
class StateManager {

    private val _playState = MutableLiveData(false)
    val playState = _playState as LiveData<Boolean>

    fun setPlay(isPlay: Boolean) {
        _playState.value = isPlay
    }


}
