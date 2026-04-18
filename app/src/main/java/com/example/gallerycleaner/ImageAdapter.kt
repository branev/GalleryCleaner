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
import com.example.gallerycleaner.databinding.ItemDateHeaderBinding
import com.example.gallerycleaner.databinding.ItemImageBinding

class ImageAdapter(
    private val onItemClick: (MediaItem) -> Unit,
    private val onItemLongClick: (MediaItem) -> Unit
) : ListAdapter<GridItem, RecyclerView.ViewHolder>(Diff) {

    private var selectedItems: Set<Uri> = emptySet()
    private var isSelectionMode: Boolean = false
    private var viewedItems: Set<Uri> = emptySet()

    var thumbnailSize: Int = 300

    companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_MEDIA = 1

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

    object Diff : DiffUtil.ItemCallback<GridItem>() {
        override fun areItemsTheSame(oldItem: GridItem, newItem: GridItem): Boolean = when {
            oldItem is GridItem.Header && newItem is GridItem.Header ->
                oldItem.bucketId == newItem.bucketId
            oldItem is GridItem.Media && newItem is GridItem.Media ->
                oldItem.item.uri == newItem.item.uri
            else -> false
        }

        override fun areContentsTheSame(oldItem: GridItem, newItem: GridItem): Boolean =
            oldItem == newItem
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is GridItem.Header -> VIEW_TYPE_HEADER
        is GridItem.Media -> VIEW_TYPE_MEDIA
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> HeaderVH(ItemDateHeaderBinding.inflate(inflater, parent, false))
            else -> ImageVH(ItemImageBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is GridItem.Header -> (holder as HeaderVH).bind(item)
            is GridItem.Media -> (holder as ImageVH).bind(item.item)
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty() || holder !is ImageVH) {
            super.onBindViewHolder(holder, position, payloads)
            return
        }
        val mediaItem = (getItem(position) as? GridItem.Media)?.item ?: return
        for (payload in payloads) {
            when (payload) {
                PAYLOAD_SELECTION_MODE, PAYLOAD_SELECTION_STATE -> {
                    holder.updateSelectionVisuals(mediaItem)
                    holder.updateViewedVisuals(mediaItem)
                }
                PAYLOAD_VIEWED_STATE -> holder.updateViewedVisuals(mediaItem)
            }
        }
    }

    /** Utility: unpack media items from the current list (headers excluded). */
    fun mediaItemsInOrder(): List<MediaItem> =
        currentList.filterIsInstance<GridItem.Media>().map { it.item }

    fun updateSelectionState(selectedItems: Set<Uri>, isSelectionMode: Boolean) {
        val oldSelected = this.selectedItems
        val oldMode = this.isSelectionMode

        this.selectedItems = selectedItems
        this.isSelectionMode = isSelectionMode

        if (oldMode != isSelectionMode) {
            notifyItemRangeChanged(0, itemCount, PAYLOAD_SELECTION_MODE)
        } else if (oldSelected != selectedItems) {
            val changedUris = (oldSelected - selectedItems) + (selectedItems - oldSelected)
            for (i in 0 until itemCount) {
                val gi = getItem(i)
                if (gi is GridItem.Media && gi.item.uri in changedUris) {
                    notifyItemChanged(i, PAYLOAD_SELECTION_STATE)
                }
            }
        }
    }

    fun updateViewedItems(viewedItems: Set<Uri>) {
        val oldViewed = this.viewedItems
        this.viewedItems = viewedItems
        val newlyViewed = viewedItems - oldViewed
        for (i in 0 until itemCount) {
            val gi = getItem(i)
            if (gi is GridItem.Media && gi.item.uri in newlyViewed) {
                notifyItemChanged(i, PAYLOAD_VIEWED_STATE)
            }
        }
    }

    inner class HeaderVH(private val binding: ItemDateHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(header: GridItem.Header) {
            binding.headerTitle.text = header.title
            binding.headerCount.text = binding.root.resources
                .getQuantityString(R.plurals.header_item_count, header.count, header.count)
        }
    }

    inner class ImageVH(private val binding: ItemImageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    (getItem(position) as? GridItem.Media)?.let { onItemClick(it.item) }
                }
            }
            binding.root.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    (getItem(position) as? GridItem.Media)?.let {
                        onItemLongClick(it.item)
                        return@setOnLongClickListener true
                    }
                }
                false
            }
        }

        fun bind(item: MediaItem) {
            binding.imageView.setImageDrawable(null)
            binding.imageView.load(item.uri) {
                crossfade(true)
                size(thumbnailSize)
                if (item.mediaType == MediaType.VIDEO) {
                    decoderFactory { result, options, _ ->
                        VideoFrameDecoder(result.source, options)
                    }
                    videoFrameMillis(1000)
                }
            }

            val label = when (item.source) {
                SourceType.OTHER -> item.bucket ?: SourceType.OTHER.label
                else -> item.source.label
            }
            binding.sourceBadge.text = label

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
            binding.selectionRing.visibility = if (isSelected) View.VISIBLE else View.GONE
            binding.selectionTint.visibility = if (isSelected) View.VISIBLE else View.GONE
            binding.checkMark.visibility = if (isSelected) View.VISIBLE else View.GONE
            binding.dimOverlay.visibility =
                if (isSelectionMode && !isSelected) View.VISIBLE else View.GONE
            binding.sourceBadge.visibility =
                if (isSelectionMode && !isSelected) View.GONE else View.VISIBLE
        }
    }
}
