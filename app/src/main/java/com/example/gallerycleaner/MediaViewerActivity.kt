package com.example.gallerycleaner

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.format.Formatter
import android.view.View
import android.widget.MediaController
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.example.gallerycleaner.databinding.ActivityMediaViewerBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MediaViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMediaViewerBinding
    private var isVideo = false
    private var isPlaying = false

    companion object {
        const val EXTRA_URI = "extra_uri"
        const val EXTRA_MEDIA_TYPE = "extra_media_type"
        const val EXTRA_DISPLAY_NAME = "extra_display_name"
        const val EXTRA_DATE_ADDED = "extra_date_added"
        const val EXTRA_SIZE = "extra_size"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMediaViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_URI, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_URI)
        }
        val mediaTypeOrdinal = intent.getIntExtra(EXTRA_MEDIA_TYPE, 0)
        val displayName = intent.getStringExtra(EXTRA_DISPLAY_NAME) ?: ""
        val dateAdded = intent.getLongExtra(EXTRA_DATE_ADDED, 0L)
        val size = intent.getLongExtra(EXTRA_SIZE, 0L)

        if (uri == null) {
            finish()
            return
        }

        isVideo = mediaTypeOrdinal == MediaType.VIDEO.ordinal

        setupToolbar(displayName, dateAdded, size)

        if (isVideo) {
            setupVideoPlayer(uri)
        } else {
            setupImageViewer(uri)
        }
    }

    private fun setupToolbar(displayName: String, dateAdded: Long, size: Long) {
        binding.viewerTitle.text = displayName

        val parts = mutableListOf<String>()
        if (dateAdded > 0) {
            val date = Date(dateAdded * 1000L)
            parts.add(SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date))
        }
        if (size > 0) {
            parts.add(Formatter.formatFileSize(this, size))
        }
        if (parts.isNotEmpty()) {
            binding.viewerSubtitle.text = parts.joinToString(" • ")
        }

        binding.btnClose.setOnClickListener {
            finish()
        }
    }

    private fun setupImageViewer(uri: Uri) {
        binding.imageView.visibility = View.VISIBLE
        binding.videoContainer.visibility = View.GONE
        binding.videoControlsOverlay.visibility = View.GONE

        binding.imageView.load(uri) {
            crossfade(true)
            listener(
                onStart = {
                    binding.progressBar.visibility = View.VISIBLE
                },
                onSuccess = { _, _ ->
                    binding.progressBar.visibility = View.GONE
                },
                onError = { _, _ ->
                    binding.progressBar.visibility = View.GONE
                }
            )
        }

        // Tap to toggle toolbar visibility
        binding.imageView.setOnClickListener {
            toggleToolbar()
        }
    }

    private fun setupVideoPlayer(uri: Uri) {
        binding.imageView.visibility = View.GONE
        binding.videoContainer.visibility = View.VISIBLE
        binding.videoControlsOverlay.visibility = View.VISIBLE
        binding.progressBar.visibility = View.VISIBLE

        binding.videoView.setVideoURI(uri)

        // Use system MediaController for full controls
        val mediaController = MediaController(this)
        mediaController.setAnchorView(binding.videoView)
        binding.videoView.setMediaController(mediaController)

        binding.videoView.setOnPreparedListener { mp ->
            binding.progressBar.visibility = View.GONE
            mp.isLooping = false

            // Scale video to fit entirely within screen while maintaining aspect ratio
            scaleVideoToFit(mp.videoWidth, mp.videoHeight)

            updatePlayPauseIcon()
        }

        binding.videoView.setOnCompletionListener {
            isPlaying = false
            updatePlayPauseIcon()
        }

        binding.videoView.setOnErrorListener { _, _, _ ->
            binding.progressBar.visibility = View.GONE
            true
        }

        // Play/pause button
        binding.btnPlayPause.setOnClickListener {
            togglePlayPause()
        }

        // Tap on video overlay to toggle play/pause
        binding.videoControlsOverlay.setOnClickListener {
            togglePlayPause()
        }
    }

    private fun togglePlayPause() {
        if (binding.videoView.isPlaying) {
            binding.videoView.pause()
            isPlaying = false
        } else {
            binding.videoView.start()
            isPlaying = true
        }
        updatePlayPauseIcon()
    }

    private fun updatePlayPauseIcon() {
        if (isPlaying || binding.videoView.isPlaying) {
            binding.btnPlayPause.setImageResource(R.drawable.ic_pause_circle)
        } else {
            binding.btnPlayPause.setImageResource(R.drawable.ic_play_circle)
        }
    }

    private fun scaleVideoToFit(videoWidth: Int, videoHeight: Int) {
        val containerWidth = binding.videoContainer.width
        val containerHeight = binding.videoContainer.height

        if (videoWidth > 0 && videoHeight > 0 && containerWidth > 0 && containerHeight > 0) {
            val videoAspect = videoWidth.toFloat() / videoHeight
            val containerAspect = containerWidth.toFloat() / containerHeight

            val layoutParams = binding.videoView.layoutParams
            if (videoAspect > containerAspect) {
                // Video is wider than container - fit to container width
                layoutParams.width = containerWidth
                layoutParams.height = (containerWidth / videoAspect).toInt()
            } else {
                // Video is taller than container - fit to container height
                layoutParams.height = containerHeight
                layoutParams.width = (containerHeight * videoAspect).toInt()
            }
            binding.videoView.layoutParams = layoutParams
        }
    }

    private fun toggleToolbar() {
        if (binding.topInfoBar.visibility == View.VISIBLE) {
            binding.topInfoBar.visibility = View.GONE
        } else {
            binding.topInfoBar.visibility = View.VISIBLE
        }
    }

    override fun onPause() {
        super.onPause()
        if (isVideo && binding.videoView.isPlaying) {
            binding.videoView.pause()
            isPlaying = false
            updatePlayPauseIcon()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isVideo) {
            binding.videoView.stopPlayback()
        }
    }
}
