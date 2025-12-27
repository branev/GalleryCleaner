package com.example.gallerycleaner

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gallerycleaner.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: GalleryViewModel by viewModels()

    private val adapter = ImageAdapter(
        onItemClick = { item -> handleItemClick(item) },
        onItemLongClick = { item -> handleItemLongClick(item) }
    )

    private val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            viewModel.exitSelectionMode()
        }
    }

    // Back press to exit app with confirmation
    private var backPressedOnce = false
    private val exitBackCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (backPressedOnce) {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            } else {
                backPressedOnce = true
                showSnackbar(getString(R.string.press_back_again_to_exit))
                binding.root.postDelayed({ backPressedOnce = false }, 2000)
            }
        }
    }

    // Track previous filter state to detect filter changes
    private var previousMediaTypes: Set<MediaType>? = null
    private var previousSources: Set<SourceType>? = null
    private var previousDateRange: DateRange? = null
    private var previousSortOption: SortOption? = null

    // Track items pending trash (for result handling)
    private var pendingTrashUris: Set<Uri> = emptySet()
    // Track total size of items pending trash (for snackbar message)
    private var pendingTrashSize: Long = 0L
    // Track if this is a trash or restore operation
    private var isRestoreOperation: Boolean = false

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val granted = result.values.any { it }
            if (granted) viewModel.loadMedia()
            else showError(getString(R.string.permission_denied))
        }


    // Trash/Restore request launcher for Android 11+ (API 30+)
    private val trashRequestLauncher: ActivityResultLauncher<IntentSenderRequest> =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            val count = pendingTrashUris.size
            if (result.resultCode == Activity.RESULT_OK) {
                if (isRestoreOperation) {
                    // Restore succeeded - reload media to show restored items
                    viewModel.loadMedia()
                    showSnackbar(getString(R.string.restore_success, count))
                } else {
                    // Trash succeeded - remove from view and show undo option
                    viewModel.removeDeletedItems(pendingTrashUris)
                    showTrashSuccessSnackbar(count, pendingTrashSize, pendingTrashUris)
                }
            } else {
                // User cancelled
                val message = if (isRestoreOperation) {
                    getString(R.string.restore_failed)
                } else {
                    getString(R.string.trash_failed)
                }
                showSnackbar(message)
            }
            pendingTrashUris = emptySet()
            pendingTrashSize = 0L
            isRestoreOperation = false
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupMediaTypeChips()
        setupFilterButton()
        setupEmptyStateClickHandler()
        setupSelectionToolbar()
        setupSelectionActionBar()
        setupContinueFab()
        setupBackHandler()
        observeUiState()
        observeViewedItems()

        if (hasReadPermission()) {
            viewModel.loadMedia()
        } else {
            requestReadPermission()
        }
    }

    private fun setupRecyclerView() {
        val layoutManager = GridLayoutManager(this, resources.getInteger(R.integer.grid_span_count))
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.adapter = adapter

        // Track items that scroll off the top as "viewed" and update FAB state
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                // Only mark items when scrolling down (items going off the top)
                if (dy > 0) {
                    markItemsAboveViewportAsViewed(layoutManager)
                }
                // Update FAB enabled state based on scroll position
                updateContinueFabState(layoutManager)
            }
        })
    }

    private fun markItemsAboveViewportAsViewed(layoutManager: GridLayoutManager) {
        val state = viewModel.uiState.value
        if (state !is GalleryUiState.Normal && state !is GalleryUiState.Selection) return

        val items = when (state) {
            is GalleryUiState.Normal -> state.items
            is GalleryUiState.Selection -> state.items
            else -> return
        }

        // Mark items that have scrolled off the top (position 0 to first visible - 1)
        val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
        if (firstVisiblePosition > 0) {
            val urisToMark = (0 until firstVisiblePosition).map { items[it].uri }
            viewModel.markItemsAsViewed(urisToMark)
        }
    }

    private fun setupMediaTypeChips() {
        binding.chipPhotos.setOnClickListener {
            viewModel.toggleMediaType(MediaType.PHOTO)
        }
        binding.chipVideos.setOnClickListener {
            viewModel.toggleMediaType(MediaType.VIDEO)
        }
    }

    private fun updateMediaTypeChips(selectedMediaTypes: Set<MediaType>) {
        binding.chipPhotos.isChecked = MediaType.PHOTO in selectedMediaTypes
        binding.chipVideos.isChecked = MediaType.VIDEO in selectedMediaTypes
    }

    private fun setupFilterButton() {
        binding.btnFilters.setOnClickListener {
            showFilterBottomSheet()
        }
    }

    private fun setupEmptyStateClickHandler() {
        binding.emptyStateContainer.setOnClickListener {
            // Open filters if in NoFiltersSelected or NoMatchingItems state
            val state = viewModel.uiState.value
            if (state is GalleryUiState.NoFiltersSelected || state is GalleryUiState.NoMatchingItems) {
                showFilterBottomSheet()
            }
        }
    }

    private fun showFilterBottomSheet() {
        val bottomSheet = FilterBottomSheetFragment.newInstance()
        bottomSheet.show(supportFragmentManager, FilterBottomSheetFragment.TAG)
    }

    private fun updateFilterSummary(
        selectedSources: Set<SourceType>,
        sourceCounts: Map<SourceType, Int>,
        selectedDateRange: DateRange,
        selectedSortOption: SortOption
    ) {
        val summaryParts = mutableListOf<String>()

        // Date range (if not default)
        if (selectedDateRange.preset != DateRangePreset.LAST_30_DAYS) {
            summaryParts.add(selectedDateRange.preset.label)
        }

        // Sources (if not all selected)
        val availableSources = sourceCounts.keys
        if (availableSources.isNotEmpty() && !selectedSources.containsAll(availableSources)) {
            val selectedCount = selectedSources.intersect(availableSources).size
            val sourceText = when {
                selectedCount == 1 -> selectedSources.first { it in availableSources }.label
                selectedCount > 1 -> "$selectedCount sources"
                else -> null
            }
            sourceText?.let { summaryParts.add(it) }
        }

        // Sort (if not default)
        if (selectedSortOption != SortOption.DATE_DESC) {
            summaryParts.add(selectedSortOption.label)
        }

        if (summaryParts.isEmpty()) {
            binding.filterSummary.visibility = View.GONE
        } else {
            binding.filterSummary.text = summaryParts.joinToString(" • ")
            binding.filterSummary.visibility = View.VISIBLE
        }
    }

    private fun setupSelectionToolbar() {
        binding.selectionToolbar.setNavigationOnClickListener {
            viewModel.exitSelectionMode()
        }
    }

    private fun setupSelectionActionBar() {
        binding.btnSelectAll.setOnClickListener { viewModel.selectAll() }
        binding.btnDelete.setOnClickListener { handleDelete() }
    }

    private fun setupBackHandler() {
        // Exit confirmation callback (lower priority - added first)
        onBackPressedDispatcher.addCallback(this, exitBackCallback)
        // Selection mode callback (higher priority - added last)
        onBackPressedDispatcher.addCallback(this, backCallback)
    }

    private fun setupContinueFab() {
        binding.fabContinue.setOnClickListener {
            scrollToFirstUnviewed()
        }
    }

    private fun scrollToFirstUnviewed() {
        val state = viewModel.uiState.value
        val items = when (state) {
            is GalleryUiState.Normal -> state.items
            is GalleryUiState.Selection -> state.items
            else -> emptyList()
        }

        val firstUnviewedIndex = viewModel.getFirstUnviewedIndex(items)
        if (firstUnviewedIndex >= 0) {
            binding.recyclerView.smoothScrollToPosition(firstUnviewedIndex)
        }
    }

    private fun updateContinueFabState(layoutManager: GridLayoutManager) {
        // Only update if FAB is visible
        if (binding.fabContinue.visibility != View.VISIBLE) return

        val state = viewModel.uiState.value
        val items = when (state) {
            is GalleryUiState.Normal -> state.items
            is GalleryUiState.Selection -> state.items
            else -> return
        }

        val firstUnviewedIndex = viewModel.getFirstUnviewedIndex(items)
        val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()

        // Disable FAB if we're already at or past the first unviewed item
        val canScrollToUnviewed = firstUnviewedIndex > lastVisiblePosition
        binding.fabContinue.isEnabled = canScrollToUnviewed
        binding.fabContinue.alpha = if (canScrollToUnviewed) 1.0f else 0.5f
    }

    private fun observeViewedItems() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Combine viewedItems with uiState to determine FAB visibility and update adapter
                combine(viewModel.viewedItems, viewModel.uiState) { viewedItems, uiState ->
                    Pair(viewedItems, uiState)
                }.collect { (viewedItems, uiState) ->
                    // Update adapter with viewed items
                    adapter.updateViewedItems(viewedItems)

                    // Determine if Continue FAB should be visible
                    val items = when (uiState) {
                        is GalleryUiState.Normal -> uiState.items
                        is GalleryUiState.Selection -> uiState.items
                        else -> emptyList()
                    }

                    // Show FAB if there are viewed items AND there are unviewed items to scroll to
                    val hasViewedItems = viewedItems.isNotEmpty()
                    val hasUnviewedItems = items.any { it.uri !in viewedItems }
                    val showFab = hasViewedItems && hasUnviewedItems &&
                            (uiState is GalleryUiState.Normal || uiState is GalleryUiState.Selection)

                    binding.fabContinue.visibility = if (showFab) View.VISIBLE else View.GONE

                    // Update FAB enabled state when visibility changes
                    if (showFab) {
                        val layoutManager = binding.recyclerView.layoutManager as? GridLayoutManager
                        layoutManager?.let { updateContinueFabState(it) }
                    }
                }
            }
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderState(state)
                }
            }
        }
    }

    private fun renderState(state: GalleryUiState) {
        when (state) {
            is GalleryUiState.Loading -> {
                binding.progressBar.visibility = View.VISIBLE
                binding.filterLoadingIndicator.visibility = View.GONE
                binding.filterSummary.visibility = View.GONE
                binding.emptyStateContainer.visibility = View.GONE
                binding.recyclerView.visibility = View.GONE
                binding.topBar.visibility = View.GONE
                binding.selectionToolbar.visibility = View.GONE
                binding.selectionActionBar.visibility = View.GONE
                backCallback.isEnabled = false
            }

            is GalleryUiState.Empty -> {
                binding.progressBar.visibility = View.GONE
                binding.filterLoadingIndicator.visibility = View.GONE
                binding.filterSummary.visibility = View.GONE
                binding.emptyStateContainer.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
                binding.topBar.visibility = View.GONE
                binding.selectionToolbar.visibility = View.GONE
                binding.selectionActionBar.visibility = View.GONE
                backCallback.isEnabled = false

                // Show generic empty state
                binding.emptyIcon.setImageResource(R.drawable.ic_filter_list)
                binding.emptyTitle.text = state.message
                binding.emptySubtitle.visibility = View.GONE
            }

            is GalleryUiState.NoFiltersSelected -> {
                binding.progressBar.visibility = View.GONE
                binding.filterLoadingIndicator.visibility = View.GONE
                binding.emptyStateContainer.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
                binding.topBar.visibility = View.VISIBLE
                binding.selectionToolbar.visibility = View.GONE
                binding.selectionActionBar.visibility = View.GONE
                backCallback.isEnabled = false

                // Show "no filters selected" state
                binding.emptyIcon.setImageResource(R.drawable.ic_filter_list)
                binding.emptyTitle.text = getString(R.string.no_filters_selected)
                binding.emptySubtitle.text = getString(R.string.select_categories_hint)
                binding.emptySubtitle.visibility = View.VISIBLE

                adapter.submitList(emptyList())
                updateMediaTypeChips(state.selectedMediaTypes)
                updateFilterSummary(state.selectedSources, state.sourceCounts, state.selectedDateRange, state.selectedSortOption)
            }

            is GalleryUiState.NoMatchingItems -> {
                binding.progressBar.visibility = View.GONE
                binding.filterLoadingIndicator.visibility = View.GONE
                binding.emptyStateContainer.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
                binding.topBar.visibility = View.VISIBLE
                binding.selectionToolbar.visibility = View.GONE
                binding.selectionActionBar.visibility = View.GONE
                backCallback.isEnabled = false

                // Show "no matching items" state
                binding.emptyIcon.setImageResource(R.drawable.ic_filter_list)
                binding.emptyTitle.text = getString(R.string.no_matching_items)
                binding.emptySubtitle.text = getString(R.string.adjust_filters_hint)
                binding.emptySubtitle.visibility = View.VISIBLE

                adapter.submitList(emptyList())
                updateMediaTypeChips(state.selectedMediaTypes)
                updateFilterSummary(state.selectedSources, state.sourceCounts, state.selectedDateRange, state.selectedSortOption)
            }

            is GalleryUiState.Normal -> {
                binding.progressBar.visibility = View.GONE
                binding.emptyStateContainer.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
                binding.topBar.visibility = View.VISIBLE
                binding.selectionToolbar.visibility = View.GONE
                binding.selectionActionBar.visibility = View.GONE

                // Check if filters changed - show loading and scroll to top if so
                val filtersChanged = previousMediaTypes != state.selectedMediaTypes ||
                        previousSources != state.selectedSources ||
                        previousDateRange != state.selectedDateRange ||
                        previousSortOption != state.selectedSortOption
                previousMediaTypes = state.selectedMediaTypes
                previousSources = state.selectedSources
                previousDateRange = state.selectedDateRange
                previousSortOption = state.selectedSortOption

                if (filtersChanged) {
                    binding.filterLoadingIndicator.visibility = View.VISIBLE
                }

                adapter.submitList(state.items) {
                    binding.filterLoadingIndicator.visibility = View.GONE
                    if (filtersChanged) {
                        binding.recyclerView.scrollToPosition(0)
                    }
                }
                adapter.updateSelectionState(emptySet(), false)
                updateMediaTypeChips(state.selectedMediaTypes)
                updateFilterSummary(state.selectedSources, state.sourceCounts, state.selectedDateRange, state.selectedSortOption)
                backCallback.isEnabled = false
            }

            is GalleryUiState.Selection -> {
                binding.progressBar.visibility = View.GONE
                binding.filterLoadingIndicator.visibility = View.GONE
                binding.emptyStateContainer.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
                binding.topBar.visibility = View.VISIBLE
                binding.selectionToolbar.visibility = View.VISIBLE
                binding.selectionActionBar.visibility = View.VISIBLE

                // Update toolbar title with selection count
                val selectedCount = state.selectedItems.size
                val title = if (state.hiddenSelectedCount > 0) {
                    getString(R.string.selected_with_hidden, selectedCount, state.hiddenSelectedCount)
                } else {
                    getString(R.string.selected_count, selectedCount)
                }
                binding.selectionToolbar.title = title

                adapter.submitList(state.items)
                adapter.updateSelectionState(state.selectedItems, true)
                updateMediaTypeChips(state.selectedMediaTypes)
                updateFilterSummary(state.selectedSources, state.sourceCounts, state.selectedDateRange, state.selectedSortOption)
                backCallback.isEnabled = true
            }
        }
    }

    private fun handleItemClick(item: MediaItem) {
        val state = viewModel.uiState.value
        if (state is GalleryUiState.Selection) {
            viewModel.toggleItemSelection(item.uri)
        } else {
            // Normal mode: open media viewer
            openMediaViewer(item)
        }
    }

    private fun openMediaViewer(item: MediaItem) {
        val intent = Intent(this, MediaViewerActivity::class.java).apply {
            putExtra(MediaViewerActivity.EXTRA_URI, item.uri)
            putExtra(MediaViewerActivity.EXTRA_MEDIA_TYPE, item.mediaType.ordinal)
            putExtra(MediaViewerActivity.EXTRA_DISPLAY_NAME, item.displayName)
        }
        startActivity(intent)
    }

    private fun handleItemLongClick(item: MediaItem) {
        val state = viewModel.uiState.value
        if (state !is GalleryUiState.Selection) {
            viewModel.enterSelectionMode(item.uri)
        }
    }

    private fun handleDelete() {
        val selectedItems = viewModel.getSelectedItems()
        if (selectedItems.isEmpty()) return

        val state = viewModel.uiState.value
        val hiddenCount = if (state is GalleryUiState.Selection) state.hiddenSelectedCount else 0

        showTrashConfirmationDialog(selectedItems.size, hiddenCount) {
            performTrash(selectedItems)
        }
    }

    private fun showTrashConfirmationDialog(count: Int, hiddenCount: Int, onConfirm: () -> Unit) {
        val message = if (hiddenCount > 0) {
            getString(R.string.trash_confirmation_message_with_hidden, hiddenCount)
        } else {
            getString(R.string.trash_confirmation_message)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.trash_confirmation_title, count))
            .setMessage(message)
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.move_to_trash)) { _, _ ->
                onConfirm()
            }
            .show()
    }

    private fun performTrash(uris: Set<Uri>) {
        pendingTrashUris = uris
        pendingTrashSize = viewModel.getSelectedItemsTotalSize()
        isRestoreOperation = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ uses createTrashRequest for soft delete
            val pendingIntent = MediaStore.createTrashRequest(contentResolver, uris.toList(), true)
            val request = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
            trashRequestLauncher.launch(request)
        } else {
            // Pre-Android 11: delete directly (no system trash available)
            val totalSize = pendingTrashSize
            var deletedCount = 0
            for (uri in uris) {
                try {
                    val rowsDeleted = contentResolver.delete(uri, null, null)
                    if (rowsDeleted > 0) deletedCount++
                } catch (e: SecurityException) {
                    // Skip items we can't delete
                }
            }

            if (deletedCount == uris.size) {
                viewModel.removeDeletedItems(uris)
                showSnackbar(getString(R.string.trash_success_with_size, deletedCount, formatFileSize(totalSize)))
            } else if (deletedCount > 0) {
                viewModel.removeDeletedItems(uris)
                showSnackbar(getString(R.string.trash_partial, deletedCount, uris.size))
            } else {
                showSnackbar(getString(R.string.trash_failed))
            }
            pendingTrashUris = emptySet()
            pendingTrashSize = 0L
        }
    }

    private fun performRestore(uris: Set<Uri>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            pendingTrashUris = uris
            isRestoreOperation = true
            // createTrashRequest with isTrashed=false restores items
            val pendingIntent = MediaStore.createTrashRequest(contentResolver, uris.toList(), false)
            val request = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
            trashRequestLauncher.launch(request)
        }
        // Pre-Android 11 doesn't have system trash, so no restore is possible
    }

    private fun showTrashSuccessSnackbar(count: Int, totalSize: Long, trashedUris: Set<Uri>) {
        val message = getString(R.string.trash_success_with_size, count, formatFileSize(totalSize))
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction(getString(R.string.undo)) {
                performRestore(trashedUris)
            }
            .show()
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1_073_741_824 -> String.format("%.1f GB", bytes / 1_073_741_824.0)
            bytes >= 1_048_576 -> String.format("%.1f MB", bytes / 1_048_576.0)
            bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        binding.emptyStateContainer.visibility = View.VISIBLE
        binding.emptyIcon.setImageResource(R.drawable.ic_filter_list)
        binding.emptyTitle.text = message
        binding.emptySubtitle.visibility = View.GONE
        binding.progressBar.visibility = View.GONE
    }

    private fun hasReadPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) ==
                PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            @Suppress("DEPRECATION")
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestReadPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            permissionLauncher.launch(arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            ))
        } else {
            @Suppress("DEPRECATION")
            permissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
        }
    }
}
