package com.example.pagtest

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import com.example.pagtest.databinding.ActivityCollageBinding
import kotlinx.coroutines.launch

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
            val size = (24000 / uris.size).coerceIn(256, 1920)
            val bitmaps = uris.map { getBitmapWithSize(it, size) }

            val testBitmaps = bitmaps.map {
                TestBitmap(it)
            }

            (binding.collageView as TurboCollageView).setBitmaps(testBitmaps)

            binding.buttonRefresh.setOnClickListener {
                (binding.collageView as TurboCollageView).collage()
            }

            binding.buttonType?.setOnClickListener {
                (binding.collageView as TurboCollageView).changeType()
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

        }

    }

    private fun changRatio(ratio: String) {
        binding.collageView.updateLayoutParams<ConstraintLayout.LayoutParams> {
            dimensionRatio = ratio
        }

        binding.collageView.post {
            (binding.collageView as TurboCollageView).collage()
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