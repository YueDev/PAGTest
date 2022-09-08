package com.example.pagtest

import android.Manifest
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.pagtest.databinding.ActivityMainBinding
import com.permissionx.guolindev.PermissionX
import com.sangcomz.fishbun.FishBun

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val collageGalleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            val uris = it.data?.getParcelableArrayListExtra<Uri>(FishBun.INTENT_PATH) ?: return@registerForActivityResult
            CollageActivity.startNewInstance(this, uris)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.title = "主页"
        supportActionBar?.elevation = 0f

        window.navigationBarColor = Color.parseColor("#262626")

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            PermissionX.init(this).permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE).request {
                allGranted, _, _ ->
                if (!allGranted) {
                    Toast.makeText(this, "没有存储权限", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonCollage.setOnClickListener {
            toGallery(this, 100, collageGalleryLauncher)
        }

    }


}