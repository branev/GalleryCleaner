package com.example.gallerycleaner

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.Formatter
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import android.media.MediaMetadataRetriever
import android.util.Size
import coil.load
import com.example.gallerycleaner.databinding.ActivityMediaViewerBinding
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MediaViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMediaViewerBinding
    private var isVideo = false
    private var isPlaying = false
    private var itemUri: Uri? = null
    private val progressHandler = Handler(Looper.getMainLooper())
    private val progressUpdater = object : Runnable {
        override fun run() {
            if (binding.videoView.isPlaying) {
                val pos = binding.videoView.currentPosition
                binding.videoSeekBar.progress = pos
                binding.videoTimeCurrent.text = formatMs(pos.toLong())
            }
            progressHandler.postDelayed(this, 250L)
        }
    }

    companion object {
        const val EXTRA_URI = "extra_uri"
        const val EXTRA_MEDIA_TYPE = "extra_media_type"
        const val EXTRA_DISPLAY_NAME = "extra_display_name"
        const val EXTRA_DATE_ADDED = "extra_date_added"
        const val EXTRA_SIZE = "extra_size"
        const val EXTRA_SOURCE = "extra_source"

        const val EXTRA_ACTION = "extra_action"
        const val ACTION_KEPT = "kept"
        const val ACTION_DELETE = "delete"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMediaViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val originalPaddingTop = binding.topInfoBar.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(binding.topInfoBar) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val topInset = maxOf(systemBars.top, cutout.top)
            view.setPadding(view.paddingLeft, originalPaddingTop + topInset, view.paddingRight, view.paddingBottom)
            insets
        }

        val originalActionBarPaddingBottom = binding.bottomActionBar.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomActionBar) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                view.paddingLeft, view.paddingTop, view.paddingRight,
                originalActionBarPaddingBottom + systemBars.bottom
            )
            insets
        }

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
        val sourceOrdinal = intent.getIntExtra(EXTRA_SOURCE, -1)

        if (uri == null) {
            finish()
            return
        }
        itemUri = uri
        isVideo = mediaTypeOrdinal == MediaType.VIDEO.ordinal

        setupToolbar(displayName, dateAdded, size, sourceOrdinal)
        setupActionBar(uri)

        if (isVideo) {
            setupVideoPlayer(uri)
        } else {
            setupImageViewer(uri)
        }
    }

    private fun setupToolbar(displayName: String, dateAdded: Long, size: Long, sourceOrdinal: Int) {
        binding.viewerTitle.text = displayName

        val parts = mutableListOf<String>()
        if (dateAdded > 0) {
            val date = Date(dateAdded * 1000L)
            parts.add(SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date))
        }
        if (size > 0) {
            parts.add(Formatter.formatFileSize(this, size))
        }
        if (sourceOrdinal in 0 until SourceType.values().size) {
            parts.add(SourceType.values()[sourceOrdinal].label)
        }
        binding.viewerSubtitle.text = parts.joinToString(" · ")

        binding.btnClose.setOnClickListener { finish() }
    }

    private fun setupActionBar(uri: Uri) {
        binding.btnOverlayInfo.setOnClickListener {
            Snackbar.make(binding.root, R.string.viewer_info_coming_soon, Snackbar.LENGTH_SHORT).show()
        }
        binding.btnOverlayKeep.setOnClickListener { view ->
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            val result = Intent().apply {
                putExtra(EXTRA_ACTION, ACTION_KEPT)
                putExtra(EXTRA_URI, uri)
            }
            setResult(RESULT_OK, result)
            finish()
        }
        binding.btnOverlayDelete.setOnClickListener {
            val result = Intent().apply {
                putExtra(EXTRA_ACTION, ACTION_DELETE)
                putExtra(EXTRA_URI, uri)
            }
            setResult(RESULT_OK, result)
            finish()
        }
    }

    private fun setupImageViewer(uri: Uri) {
        binding.imageView.visibility = View.VISIBLE
        binding.videoContainer.visibility = View.GONE
        binding.videoPlayerRow.visibility = View.GONE
        binding.btnCenterPlay.visibility = View.GONE

        binding.imageView.load(uri) {
            crossfade(true)
            listener(
                onStart = { binding.progressBar.visibility = View.VISIBLE },
                onSuccess = { _, _ -> binding.progressBar.visibility = View.GONE },
                onError = { _, _ -> binding.progressBar.visibility = View.GONE },
            )
        }

        binding.imageView.setOnClickListener { toggleChrome() }
    }

    private fun setupVideoPlayer(uri: Uri) {
        // imageView carries the poster. Keep videoContainer GONE until the
        // video is prepared — its internal SurfaceView otherwise paints a
        // black hole on top of the poster during warmup.
        binding.imageView.visibility = View.VISIBLE
        loadVideoPoster(uri)

        binding.videoContainer.visibility = View.INVISIBLE
        binding.videoPlayerRow.visibility = View.VISIBLE
        binding.progressBar.visibility = View.VISIBLE

        binding.videoView.setVideoURI(uri)

        binding.videoView.setOnPreparedListener { mp ->
            binding.progressBar.visibility = View.GONE
            mp.isLooping = false
            scaleVideoToFit(mp.videoWidth, mp.videoHeight)
            val duration = binding.videoView.duration
            binding.videoSeekBar.max = duration
            binding.videoTimeTotal.text = formatMs(duration.toLong())
            // Keep videoContainer INVISIBLE — its SurfaceView punches a
            // black hole over the poster until an actual frame is drawn.
            // We flip it visible on first play (below).
            binding.btnCenterPlay.visibility = View.VISIBLE
            updatePlayPauseIcon()
        }

        binding.videoView.setOnCompletionListener {
            isPlaying = false
            binding.btnCenterPlay.visibility = View.VISIBLE
            updatePlayPauseIcon()
            progressHandler.removeCallbacks(progressUpdater)
        }

        binding.videoView.setOnErrorListener { _, _, _ ->
            binding.progressBar.visibility = View.GONE
            true
        }

        binding.btnPlayPause.setOnClickListener { togglePlayPause() }
        binding.btnCenterPlay.setOnClickListener { togglePlayPause() }
        // Tap on the video itself toggles play/pause (YouTube-style).
        // Chrome stays visible for videos so the seekbar and actions remain
        // reachable.
        binding.videoContainer.setOnClickListener { togglePlayPause() }

        binding.videoSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.videoView.seekTo(progress)
                    binding.videoTimeCurrent.text = formatMs(progress.toLong())
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
    }

    private fun togglePlayPause() {
        if (binding.videoView.isPlaying) {
            binding.videoView.pause()
            isPlaying = false
            progressHandler.removeCallbacks(progressUpdater)
            binding.btnCenterPlay.visibility = View.VISIBLE
        } else {
            // Reveal the video surface the moment we start playing — by
            // the time it's drawn the first frame is written, so no black
            // hole flash.
            binding.videoContainer.visibility = View.VISIBLE
            binding.videoView.start()
            isPlaying = true
            progressHandler.post(progressUpdater)
            binding.btnCenterPlay.visibility = View.GONE
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
                layoutParams.width = containerWidth
                layoutParams.height = (containerWidth / videoAspect).toInt()
            } else {
                layoutParams.height = containerHeight
                layoutParams.width = (containerHeight * videoAspect).toInt()
            }
            binding.videoView.layoutParams = layoutParams
        }
    }

    private fun toggleChrome() {
        // Only photos toggle chrome; videos keep it visible so playback
        // controls and action buttons are always reachable.
        if (isVideo) return
        val visible = binding.topInfoBar.visibility == View.VISIBLE
        val nextVis = if (visible) View.GONE else View.VISIBLE
        binding.topInfoBar.visibility = nextVis
        binding.bottomActionBar.visibility = nextVis
    }

    private fun loadVideoPoster(uri: Uri) {
        Thread {
            val bitmap = extractVideoPoster(uri) ?: return@Thread
            runOnUiThread {
                if (!isFinishing && !isDestroyed) {
                    binding.imageView.setImageBitmap(bitmap)
                }
            }
        }.start()
    }

    private fun extractVideoPoster(uri: Uri): android.graphics.Bitmap? {
        // Fast path: MediaStore's cached thumbnail (API 29+). Often null
        // for videos whose thumbnail hasn't been generated yet — then we
        // fall through to the frame retriever.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                contentResolver.loadThumbnail(uri, Size(1024, 1024), null)
            }.getOrNull()?.let { return it }
        }
        // Fallback: platform-picked representative frame. More reliable than
        // asking for t=0 on codecs where the first frame isn't a sync frame.
        var retriever: MediaMetadataRetriever? = null
        return try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(this, uri)
            retriever.frameAtTime
                ?: retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST)
        } catch (_: Exception) {
            null
        } finally {
            try { retriever?.release() } catch (_: Exception) {}
        }
    }

    private fun formatMs(ms: Long): String {
        val totalSec = ms / 1000
        val m = totalSec / 60
        val s = totalSec % 60
        return "%d:%02d".format(m, s)
    }

    override fun onPause() {
        super.onPause()
        if (isVideo && binding.videoView.isPlaying) {
            binding.videoView.pause()
            isPlaying = false
            progressHandler.removeCallbacks(progressUpdater)
            updatePlayPauseIcon()
        }
    }

    override fun onDestroy() {
        progressHandler.removeCallbacks(progressUpdater)
        super.onDestroy()
        if (isVideo) binding.videoView.stopPlayback()
    }
}
