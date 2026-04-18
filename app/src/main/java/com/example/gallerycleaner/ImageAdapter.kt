package com.example.gallerycleaner

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.decode.VideoFrameDecoder
import coil.load
import coil.request.videoFrameMillis
import com.example.gallerycleaner.databinding.ItemImageBinding

class ImageAdapter(
    private val onItemClick: (MediaItem) -> Unit,
    private val onItemLongClick: (MediaItem) -> Unit
) : ListAdapter<MediaItem, ImageAdapter.VH>(Diff) {

    // Selection state
    private var selectedItems: Set<Uri> = emptySet()
    private var isSelectionMode: Boolean = false

    // Viewed items state
    private var viewedItems: Set<Uri> = emptySet()

    // Thumbnail loading size (adjusts with pinch-to-zoom)
    var thumbnailSize: Int = 300

    object Diff : DiffUtil.ItemCallback<MediaItem>() {
        override fun areItemsTheSame(oldItem: MediaItem, newItem: MediaItem) =
            oldItem.uri == newItem.uri

        override fun areContentsTheSame(oldItem: MediaItem, newItem: MediaItem) =
            oldItem == newItem
    }

    fun updateSelectionState(selectedItems: Set<Uri>, isSelectionMode: Boolean) {
        val oldSelected = this.selectedItems
        val oldMode = this.isSelectionMode

        this.selectedItems = selectedItems
        this.isSelectionMode = isSelectionMode

        // Notify changes for efficient partial updates
        if (oldMode != isSelectionMode) {
            // Mode changed - update all items
            notifyItemRangeChanged(0, itemCount, PAYLOAD_SELECTION_MODE)
        } else if (oldSelected != selectedItems) {
            // Only selection changed - update affected items
            val changedUris = (oldSelected - selectedItems) + (selectedItems - oldSelected)
            for (i in 0 until itemCount) {
                if (getItem(i).uri in changedUris) {
                    notifyItemChanged(i, PAYLOAD_SELECTION_STATE)
                }
            }
        }
    }

    fun updateViewedItems(viewedItems: Set<Uri>) {
        val oldViewed = this.viewedItems
        this.viewedItems = viewedItems

        // Update only newly viewed items
        val newlyViewed = viewedItems - oldViewed
        for (i in 0 until itemCount) {
            if (getItem(i).uri in newlyViewed) {
                notifyItemChanged(i, PAYLOAD_VIEWED_STATE)
            }
        }
    }

    inner class VH(private val binding: ItemImageBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }

            binding.root.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemLongClick(getItem(position))
                    true
                } else false
            }
        }

        fun bind(item: MediaItem) {
            // Clear previous image to prevent showing stale content during recycling
            binding.imageView.setImageDrawable(null)
            binding.imageView.load(item.uri) {
                crossfade(true)
                size(thumbnailSize)
                // For videos, use VideoFrameDecoder to extract a thumbnail frame
                if (item.mediaType == MediaType.VIDEO) {
                    decoderFactory { result, options, _ ->
                        VideoFrameDecoder(result.source, options)
                    }
                    videoFrameMillis(1000) // Get frame at 1 second
                }
            }

            val label = when (item.source) {
                SourceType.OTHER -> item.bucket ?: SourceType.OTHER.label
                else -> item.source.label
            }
            binding.sourceBadge.text = label

            // Show video duration badge for videos
            if (item.mediaType == MediaType.VIDEO) {
                binding.videoDurationBadge.visibility = View.VISIBLE
                binding.videoDuration.text = formatDuration(item.duration)
            } else {
                binding.videoDurationBadge.visibility = View.GONE
            }

            updateSelectionVisuals(item)
            updateViewedVisuals(item)
        }

        fun updateViewedVisuals(item: MediaItem) {
            val isViewed = item.uri in viewedItems
            // Gentle fade on the photo only — badges stay at full contrast.
            // During selection mode, selection dim overrides the reviewed fade.
            if (isViewed && !isSelectionMode) {
                binding.imageView.alpha = 0.85f
                val matrix = ColorMatrix().apply { setSaturation(0.75f) }
                binding.imageView.colorFilter = ColorMatrixColorFilter(matrix)
            } else {
                binding.imageView.alpha = 1.0f
                binding.imageView.colorFilter = null
            }
        }

        fun updateSelectionVisuals(item: MediaItem) {
            val isSelected = item.uri in selectedItems

            // Ring + warm tint + check circle on the selected tile
            binding.selectionRing.visibility = if (isSelected) View.VISIBLE else View.GONE
            binding.selectionTint.visibility = if (isSelected) View.VISIBLE else View.GONE
            binding.checkMark.visibility = if (isSelected) View.VISIBLE else View.GONE

            // 55% white dim on NON-selected tiles while in selection mode
            binding.dimOverlay.visibility =
                if (isSelectionMode && !isSelected) View.VISIBLE else View.GONE

            // Hide source badge on dimmed tiles only — on selected ones the photo is still visible
            binding.sourceBadge.visibility =
                if (isSelectionMode && !isSelected) View.GONE else View.VISIBLE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: VH, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            val item = getItem(position)
            // Partial bind for state changes
            for (payload in payloads) {
                when (payload) {
                    PAYLOAD_SELECTION_MODE, PAYLOAD_SELECTION_STATE -> {
                        holder.updateSelectionVisuals(item)
                        holder.updateViewedVisuals(item) // Opacity depends on selection mode
                    }
                    PAYLOAD_VIEWED_STATE -> holder.updateViewedVisuals(item)
                }
            }
        }
    }

    companion object {
        private const val PAYLOAD_SELECTION_MODE = "selection_mode"
        private const val PAYLOAD_SELECTION_STATE = "selection_state"
        private const val PAYLOAD_VIEWED_STATE = "viewed_state"

        fun formatDuration(durationMs: Long): String {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }
    }
}
