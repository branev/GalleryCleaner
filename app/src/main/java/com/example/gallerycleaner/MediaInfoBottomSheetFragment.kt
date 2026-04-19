package com.example.gallerycleaner

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.format.Formatter
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowCompat
import coil.load
import coil.request.videoFrameMillis
import com.example.gallerycleaner.databinding.BottomSheetMediaInfoBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MediaInfoBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetMediaInfoBinding? = null
    private val binding get() = _binding!!

    private var resolutionValueView: TextView? = null
    private var capturedValueView: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = BottomSheetMediaInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = requireArguments()
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            args.getParcelable(ARG_URI, Uri::class.java)
        } else {
            @Suppress("DEPRECATION") args.getParcelable(ARG_URI) as? Uri
        } ?: run { dismiss(); return }

        val displayName = args.getString(ARG_DISPLAY_NAME).orEmpty()
        val mediaType = MediaType.values()[args.getInt(ARG_MEDIA_TYPE, 0)]
        val source = SourceType.values()[args.getInt(ARG_SOURCE, SourceType.OTHER.ordinal)]
        val size = args.getLong(ARG_SIZE, 0L)
        val dateAdded = args.getLong(ARG_DATE_ADDED, 0L)
        val duration = args.getLong(ARG_DURATION, 0L)
        val path = args.getString(ARG_PATH).orEmpty()

        setupHeader(uri, displayName, mediaType, source)
        setupDetails(mediaType, size, duration, dateAdded, source, path)

        loadExtraMetadata(uri, dateAdded)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { w ->
            WindowCompat.getInsetsController(w, w.decorView)
                .isAppearanceLightStatusBars = false
        }
    }

    override fun onDestroyView() {
        requireActivity().window.let { w ->
            WindowCompat.getInsetsController(w, w.decorView)
                .isAppearanceLightStatusBars = true
        }
        super.onDestroyView()
        _binding = null
    }

    private fun setupHeader(uri: Uri, displayName: String, mediaType: MediaType, source: SourceType) {
        binding.infoFilename.text = displayName
        val typeLabel = if (mediaType == MediaType.VIDEO) {
            getString(R.string.info_video)
        } else {
            getString(R.string.info_photo)
        }
        binding.infoSubtitle.text =
            getString(R.string.info_media_type_and_source, typeLabel, source.label)

        binding.infoThumb.load(uri) {
            if (mediaType == MediaType.VIDEO) videoFrameMillis(0L)
            crossfade(true)
        }
        binding.infoThumbVideoBadge.visibility =
            if (mediaType == MediaType.VIDEO) View.VISIBLE else View.GONE
    }

    private fun setupDetails(
        mediaType: MediaType,
        size: Long,
        duration: Long,
        dateAdded: Long,
        source: SourceType,
        path: String,
    ) {
        binding.infoDetailsContainer.removeAllViews()

        val mono: Typeface? = ResourcesCompat.getFont(requireContext(), R.font.jetbrains_mono)

        addRow(R.string.info_size, Formatter.formatFileSize(requireContext(), size))
        if (mediaType == MediaType.VIDEO) {
            addRow(R.string.info_duration, formatMs(duration), typeface = mono)
        }
        resolutionValueView = addRow(
            R.string.info_resolution,
            getString(R.string.info_unknown),
            typeface = mono,
        )
        capturedValueView = addRow(R.string.info_captured, formatDate(dateAdded))
        addRow(R.string.info_source, source.label)
        val resolvedPath = path.ifEmpty { getString(R.string.info_unknown) }
        addRow(
            R.string.info_path,
            resolvedPath,
            typeface = mono,
            isLast = true,
            actionIcon = R.drawable.ic_copy.takeIf { path.isNotEmpty() },
            actionDescription = R.string.info_copy_path,
            onAction = if (path.isNotEmpty()) {
                { copyToClipboard(resolvedPath) }
            } else null,
        )
    }

    private fun copyToClipboard(value: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE)
            as? ClipboardManager ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText("path", value))
        Snackbar.make(
            binding.root,
            getString(R.string.info_path_copied),
            Snackbar.LENGTH_SHORT,
        ).show()
    }

    private fun addRow(
        labelRes: Int,
        value: String,
        typeface: Typeface? = null,
        isLast: Boolean = false,
        actionIcon: Int? = null,
        actionDescription: Int? = null,
        onAction: (() -> Unit)? = null,
    ): TextView {
        val ctx = requireContext()
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
            setPadding(dp(20), dp(10), dp(20), dp(10))
        }
        val label = TextView(ctx).apply {
            setText(labelRes)
            textSize = 13f
            setTextColor(ContextCompat.getColor(ctx, R.color.ink3))
            layoutParams = LinearLayout.LayoutParams(dp(96), LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        val valueView = TextView(ctx).apply {
            text = value
            textSize = 13f
            setTextColor(ContextCompat.getColor(ctx, R.color.ink))
            if (typeface != null) this.typeface = typeface
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
            setTextIsSelectable(true)
        }
        row.addView(label)
        row.addView(valueView)

        if (actionIcon != null && onAction != null) {
            val action = ImageView(ctx).apply {
                setImageResource(actionIcon)
                setColorFilter(ContextCompat.getColor(ctx, R.color.ink3))
                setPadding(dp(6), dp(6), dp(6), dp(6))
                layoutParams = LinearLayout.LayoutParams(dp(32), dp(32)).apply {
                    marginStart = dp(8)
                }
                background = ContextCompat.getDrawable(ctx, android.R.color.transparent)
                isClickable = true
                isFocusable = true
                if (actionDescription != null) {
                    contentDescription = getString(actionDescription)
                }
                setOnClickListener { onAction() }
            }
            row.addView(action)
        }

        binding.infoDetailsContainer.addView(row)

        if (!isLast) {
            val divider = View(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
                )
                setBackgroundColor(ContextCompat.getColor(ctx, R.color.line))
            }
            binding.infoDetailsContainer.addView(divider)
        }
        return valueView
    }

    private fun loadExtraMetadata(uri: Uri, fallbackDate: Long) {
        Thread {
            val ctx = context ?: return@Thread
            val projection = buildList {
                add(MediaStore.MediaColumns.WIDTH)
                add(MediaStore.MediaColumns.HEIGHT)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    add(MediaStore.MediaColumns.DATE_TAKEN)
                }
            }.toTypedArray()

            var width = 0
            var height = 0
            var dateTakenMillis = 0L

            runCatching {
                ctx.contentResolver.query(uri, projection, null, null, null)?.use { c ->
                    if (c.moveToFirst()) {
                        val wIdx = c.getColumnIndex(MediaStore.MediaColumns.WIDTH)
                        val hIdx = c.getColumnIndex(MediaStore.MediaColumns.HEIGHT)
                        if (wIdx >= 0 && !c.isNull(wIdx)) width = c.getInt(wIdx)
                        if (hIdx >= 0 && !c.isNull(hIdx)) height = c.getInt(hIdx)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val dtIdx = c.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN)
                            if (dtIdx >= 0 && !c.isNull(dtIdx)) {
                                dateTakenMillis = c.getLong(dtIdx)
                            }
                        }
                    }
                }
            }

            val resolutionText = if (width > 0 && height > 0) {
                "$width × $height"
            } else {
                getString(R.string.info_unknown)
            }
            val capturedSec = if (dateTakenMillis > 0) dateTakenMillis / 1000L else fallbackDate

            activity?.runOnUiThread {
                if (_binding != null) {
                    resolutionValueView?.text = resolutionText
                    capturedValueView?.text = formatDate(capturedSec)
                }
            }
        }.start()
    }

    private fun formatDate(unixSeconds: Long): String {
        if (unixSeconds <= 0) return getString(R.string.info_unknown)
        val date = Date(unixSeconds * 1000L)
        return SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault()).format(date)
    }

    private fun formatMs(ms: Long): String {
        if (ms <= 0) return getString(R.string.info_unknown)
        val totalSec = ms / 1000
        return "%d:%02d".format(totalSec / 60, totalSec % 60)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        const val TAG = "MediaInfoBottomSheet"

        private const val ARG_URI = "arg_uri"
        private const val ARG_DISPLAY_NAME = "arg_display_name"
        private const val ARG_MEDIA_TYPE = "arg_media_type"
        private const val ARG_SOURCE = "arg_source"
        private const val ARG_SIZE = "arg_size"
        private const val ARG_DATE_ADDED = "arg_date_added"
        private const val ARG_DURATION = "arg_duration"
        private const val ARG_PATH = "arg_path"

        fun newInstance(
            uri: Uri,
            displayName: String?,
            mediaType: MediaType,
            source: SourceType,
            size: Long,
            dateAdded: Long,
            duration: Long,
            path: String?,
        ) = MediaInfoBottomSheetFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_URI, uri)
                putString(ARG_DISPLAY_NAME, displayName)
                putInt(ARG_MEDIA_TYPE, mediaType.ordinal)
                putInt(ARG_SOURCE, source.ordinal)
                putLong(ARG_SIZE, size)
                putLong(ARG_DATE_ADDED, dateAdded)
                putLong(ARG_DURATION, duration)
                putString(ARG_PATH, path)
            }
        }
    }
}
