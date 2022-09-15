package com.example.pagtest

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import com.example.pagtest.databinding.ActivityCollageBinding
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.sqrt

class CollageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCollageBinding

    private val uris = mutableListOf<Uri>()

    companion object {

        private const val KEY_COLLAGE_ACTIVITY_IMAGES = "key_collage_activity_images"

        fun startNewInstance(context: Context, imageUris: ArrayList<Uri>) {
            Intent(context, CollageActivity::class.java).let {
                it.putParcelableArrayListExtra(KEY_COLLAGE_ACTIVITY_IMAGES, imageUris)
                context.startActivity(it)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.let {
            it.title = "Collage"
            it.setDisplayHomeAsUpEnabled(true)
            it.elevation = 0f
        }

        val list = intent.getParcelableArrayListExtra<Uri>(KEY_COLLAGE_ACTIVITY_IMAGES) ?: return
        uris.addAll(list)

        binding = ActivityCollageBinding.inflate(layoutInflater)
        setContentView(binding.root)


        lifecycleScope.launch {

            val size = sqrt(2048.0 * 2048.0 / uris.size).toInt().coerceAtMost(1920)

            Log.d("YUEDEVTAG", "size:$size")

            val testBitmaps = uris.map { TestBitmap(it, getBitmapWithSize(it, size, size)) }

            binding.collageView.setBitmaps(testBitmaps, lifecycleScope)

            binding.buttonRefresh.setOnClickListener {
                animateCollage()
            }

            binding.buttonType.setOnClickListener {
                changeType()
            }

            //16:9
            binding.button2.setOnClickListener {
                changRatio("16:9")
            }
            //4:3
            binding.button3.setOnClickListener {
                changRatio("4:3")
            }
            //3:4
            binding.button4.setOnClickListener {
                changRatio("3:4")
            }
            //1:1
            binding.button5.setOnClickListener {
                changRatio("1:1")
            }
            //9:16
            binding.button6.setOnClickListener {
                changRatio("9:16")
            }

            binding.buttonSave.setOnClickListener {
                lifecycleScope.launch {
                    binding.progressBar2.visibility = View.VISIBLE
                    val bitmap = binding.collageView.getHighResBitmap(2.0f)
                    bitmap?.also {
                        val result = saveBitmapToFile(it, UUID.randomUUID().toString())
                        Toast.makeText(this@CollageActivity, "save: $result", Toast.LENGTH_SHORT).show()
                    } ?: run {
//                        Toast.makeText(this@CollageActivity, "error: bitmap null", Toast.LENGTH_SHORT).show()
                    }
                    binding.progressBar2.visibility = View.GONE
                }
            }

        }
    }

    private fun animateCollage() {
        lifecycleScope.launch {
            binding.collageView.animateCollage()
        }
    }

    private fun changeType() {
        lifecycleScope.launch {
            binding.collageView.changeType()
        }
    }

    private fun collage() {
        lifecycleScope.launch {
            binding.collageView.collage()
        }
    }

    private fun changRatio(ratio: String) {
        binding.collageView.updateLayoutParams<ConstraintLayout.LayoutParams> {
            dimensionRatio = ratio
        }

        binding.collageView.post {
            collage()
        }
    }


    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }
        else -> false
    }
}