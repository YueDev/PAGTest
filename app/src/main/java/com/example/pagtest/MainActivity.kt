package com.example.pagtest

import android.Manifest
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.pagtest.databinding.ActivityMainBinding
import com.permissionx.guolindev.PermissionX
import com.sangcomz.fishbun.FishBun
import org.libpag.PAGFile
import java.io.FileInputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var pagFileUri: Uri? = null

    private val pagFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@registerForActivityResult
        val pfd = contentResolver.openFileDescriptor(uri, "r") ?: return@registerForActivityResult
        val byteArray = FileInputStream(pfd.fileDescriptor).readBytes()
        val pagFile: PAGFile? = PAGFile.Load(byteArray)
        pagFile?.also {
            pagFileUri = uri
            toGallery(this, 99, galleryLauncher)
        } ?: run {
            Toast.makeText(this@MainActivity, "文件无效，请重新选择", Toast.LENGTH_SHORT).show()
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            val uris = it.data?.getParcelableArrayListExtra<Uri>(FishBun.INTENT_PATH) ?: return@registerForActivityResult
            pagFileUri?.let { pagUri ->
                PagActivity.startNewInstance(this, pagUri, uris)
            }

        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.title = "主页"
        supportActionBar?.elevation = 0f

        window.navigationBarColor = Color.parseColor("#262626")

        if (!PagRecorder.isHighVersion()) {
            PermissionX.init(this).permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE).request { allGranted, _, _ ->
                if (!allGranted) {
                    Toast.makeText(this, "没有存储权限", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.button.setOnClickListener {
            pagFileLauncher.launch("*/*")
        }


    }


}