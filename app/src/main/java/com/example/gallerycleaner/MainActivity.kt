package com.example.gallerycleaner

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.format.Formatter
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: GalleryViewModel by viewModels()

    private val adapter = ImageAdapter(
        onItemClick = { item -> handleItemClick(item) },
        onItemLongClick = { item -> handleItemLongClick(item) }
    )

    // Selection state before the drag started (so dragging back deselects correctly)
    private var preDragSelection: Set<Uri> = emptySet()

    private lateinit var hintPreferences: HintPreferences
    private lateinit var hintManager: HintManager

    private lateinit var fastScrollHelper: FastScrollHelper
    private lateinit var scaleGestureDetector: android.view.ScaleGestureDetector

    private val dragSelectListener = DragSelectTouchListener(
        onDragRangeChanged = { rangeStart, rangeEnd ->
            val list = adapter.currentList
            val dragUris = (rangeStart..rangeEnd)
                .filter { it in list.indices }
                .mapNotNull { (list[it] as? GridItem.Media)?.item?.uri }
                .toSet()
            viewModel.setDragSelection(dragUris, preDragSelection)
        },
        onDragStarted = {
            // Snapshot current selection before this drag modifies it
            preDragSelection = viewModel.getSelectedItems()
        }
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

    // Track previous UI state type to detect state changes (for hint dismissal)
    private var previousStateType: String? = null

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
                    showTrashSuccessCard(count, pendingTrashSize, pendingTrashUris)
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

    private val mediaViewerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) return@registerForActivityResult
            val data = result.data ?: return@registerForActivityResult
            val action = data.getStringExtra(MediaViewerActivity.EXTRA_ACTION)
                ?: return@registerForActivityResult
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                data.getParcelableExtra(MediaViewerActivity.EXTRA_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                data.getParcelableExtra(MediaViewerActivity.EXTRA_URI) as? Uri
            } ?: return@registerForActivityResult

            when (action) {
                MediaViewerActivity.ACTION_KEPT -> viewModel.markAsViewed(uri)
                MediaViewerActivity.ACTION_DELETE -> performTrash(setOf(uri))
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle edge-to-edge insets — pad top bar below status bar/cutout,
        // and bottom elements above navigation bar
        val topBarOriginalPaddingTop = binding.topBar.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val topInset = maxOf(systemBars.top, cutout.top)
            val bottomInset = systemBars.bottom

            // Top bar: push below status bar + cutout
            binding.topBar.setPadding(
                binding.topBar.paddingLeft, topBarOriginalPaddingTop + topInset,
                binding.topBar.paddingRight, binding.topBar.paddingBottom
            )

            // Bottom elements: push above navigation bar
            val bottomBarLp = binding.selectionActionBar.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            bottomBarLp.bottomMargin = 16.dp() + bottomInset
            binding.selectionActionBar.layoutParams = bottomBarLp

            val hintLp = binding.hintCard.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            hintLp.bottomMargin = 16.dp() + bottomInset
            binding.hintCard.layoutParams = hintLp

            val fabLp = binding.fabContinue.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            fabLp.bottomMargin = 24.dp() + bottomInset
            binding.fabContinue.layoutParams = fabLp

            insets
        }

        hintPreferences = HintPreferences(this)
        hintManager = HintManager(
            hintPreferences,
            binding.hintCard,
            binding.hintTitle,
            binding.hintDetail,
            binding.btnGotIt
        )

        setupRecyclerView()
        setupMediaTypeChips()
        setupFilterButton()
        setupResetFiltersButton()
        setupSelectionActionBar()
        setupContinueFab()
        setupHelpButton()
        setupBackHandler()
        observeUiState()
        observeViewedItems()
        observeActiveFilters()

        if (hasReadPermission()) {
            viewModel.loadMedia()
        } else {
            requestReadPermission()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dragSelectListener.detachFromRecyclerView()
        fastScrollHelper.detach()
        hintManager.dismiss()
        binding.deleteSuccessOverlay.animate().cancel()
        undoAnimator?.cancel()
        undoAnimator = null
    }

    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private fun setupRecyclerView() {
        val savedColumns = viewModel.gridColumnCount.value
        val layoutManager = GridLayoutManager(this, savedColumns)
        binding.recyclerView.layoutManager = layoutManager
        adapter.thumbnailSize = resources.displayMetrics.widthPixels / savedColumns
        binding.recyclerView.adapter = adapter
        dragSelectListener.attachToRecyclerView(binding.recyclerView)

        // Date-section headers span the full grid width; media tiles span 1.
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int =
                when (adapter.getItemViewType(position)) {
                    ImageAdapter.VIEW_TYPE_HEADER -> layoutManager.spanCount
                    else -> 1
                }
        }

        fastScrollHelper = FastScrollHelper(
            recyclerView = binding.recyclerView,
            track = binding.fastScrollTrack,
            thumb = binding.fastScrollThumb,
            tooltip = binding.fastScrollTooltip,
            getDateAtPosition = { position -> formatDateForPosition(position) },
            onFastScrollPositionChanged = { position ->
                // Mark all media items above the fast-scroll position as viewed (headers skipped)
                val list = adapter.currentList
                if (position > 0 && list.isNotEmpty()) {
                    val urisToMark = (0 until position.coerceAtMost(list.size))
                        .mapNotNull { (list[it] as? GridItem.Media)?.item?.uri }
                    viewModel.markItemsAsViewed(urisToMark)
                }
            }
        )
        fastScrollHelper.attach()

        // Pinch-to-zoom grid columns
        var cumulativeScale = 1.0f
        scaleGestureDetector = android.view.ScaleGestureDetector(this,
            object : android.view.ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScaleBegin(detector: android.view.ScaleGestureDetector): Boolean {
                    cumulativeScale = 1.0f
                    dragSelectListener.isPinching = true
                    return true
                }

                override fun onScaleEnd(detector: android.view.ScaleGestureDetector) {
                    // Delay re-enabling drag-select to avoid the second finger
                    // lift being interpreted as a drag start
                    binding.recyclerView.postDelayed({
                        dragSelectListener.isPinching = false
                    }, 200)
                }

                override fun onScale(detector: android.view.ScaleGestureDetector): Boolean {
                    cumulativeScale *= detector.scaleFactor
                    val currentSpan = layoutManager.spanCount

                    val newSpan = when {
                        cumulativeScale < 0.8f -> {
                            cumulativeScale = 1.0f
                            (currentSpan + 1).coerceAtMost(5)
                        }
                        cumulativeScale > 1.2f -> {
                            cumulativeScale = 1.0f
                            (currentSpan - 1).coerceAtLeast(2)
                        }
                        else -> currentSpan
                    }

                    if (newSpan != currentSpan) {
                        changeGridColumns(layoutManager, newSpan)
                    }
                    return true
                }
            })

        binding.recyclerView.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            false
        }

        // Track items that scroll off the top as "viewed" and update FAB state
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                // Only mark items when scrolling down (items going off the top)
                if (dy > 0) {
                    markItemsAboveViewportAsViewed(layoutManager)
                    // Show fast scroll hint on first scroll
                    hintManager.showHint(
                        HintPreferences.HINT_FAST_SCROLL,
                        getString(R.string.hint_fast_scroll_title),
                        getString(R.string.hint_fast_scroll_detail),
                    )
                }
                // Update FAB enabled state based on scroll position
                updateContinueFabState(layoutManager)
            }
        })
    }

    private fun markItemsAboveViewportAsViewed(layoutManager: GridLayoutManager) {
        val state = viewModel.uiState.value
        val items = state.displayedItems
        if (items.isEmpty()) return

        // Mark items that have scrolled off the top (position 0 to first visible - 1)
        val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
        val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()

        if (firstVisiblePosition > 0) {
            val urisToMark = (0 until firstVisiblePosition).map { items[it].uri }
            viewModel.markItemsAsViewed(urisToMark)
        }

        // If we've reached the bottom, also mark all visible items as viewed
        if (lastVisiblePosition >= items.size - 1 && items.isNotEmpty()) {
            val urisToMark = items.map { it.uri }
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

    private fun setupResetFiltersButton() {
        binding.btnResetFilters.setOnClickListener {
            viewModel.resetFilters()
        }
        binding.btnEditFilters.setOnClickListener {
            showFilterBottomSheet()
        }
    }

    private fun showFilterBottomSheet() {
        val bottomSheet = FilterBottomSheetFragment.newInstance()
        bottomSheet.show(supportFragmentManager, FilterBottomSheetFragment.TAG)
    }



    private fun setupSelectionActionBar() {
        binding.btnExitSelection.setOnClickListener { viewModel.exitSelectionMode() }
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

    private fun setupHelpButton() {
        binding.btnHelp.setOnClickListener {
            HelpBottomSheetFragment.newInstance().show(supportFragmentManager, HelpBottomSheetFragment.TAG)
        }
    }

    private fun scrollToFirstUnviewed() {
        val mediaItems = adapter.mediaItemsInOrder()
        val firstUnviewedMediaIndex = viewModel.getFirstUnviewedIndex(mediaItems)
        if (firstUnviewedMediaIndex < 0) return

        val targetUri = mediaItems[firstUnviewedMediaIndex].uri
        val gridPosition = adapter.currentList.indexOfFirst {
            it is GridItem.Media && it.item.uri == targetUri
        }
        if (gridPosition >= 0) {
            binding.recyclerView.smoothScrollToPosition(gridPosition)
        }
    }

    /**
     * Continue FAB is shown only when tapping it is useful:
     * - there's at least one unreviewed item ahead of the last visible row,
     * - and the app is in a state where the grid is actually displayed.
     * In every other case the FAB hides. The design's "All caught up" ghost
     * state is reserved for when every item is reviewed — not implemented
     * here because in practice users delete as they go.
     */
    private fun updateContinueFabState(layoutManager: GridLayoutManager) {
        val state = viewModel.uiState.value
        val isGridShown = state is GalleryUiState.Normal || state is GalleryUiState.Selection
        val viewedItems = viewModel.viewedItems.value
        val mediaItems = adapter.mediaItemsInOrder()

        if (!isGridShown || viewedItems.isEmpty() || mediaItems.isEmpty()) {
            binding.fabContinue.visibility = View.GONE
            return
        }

        val firstUnviewedMediaIndex = viewModel.getFirstUnviewedIndex(mediaItems)
        if (firstUnviewedMediaIndex < 0) {
            binding.fabContinue.visibility = View.GONE
            return
        }

        val targetUri = mediaItems[firstUnviewedMediaIndex].uri
        val gridPosition = adapter.currentList.indexOfFirst {
            it is GridItem.Media && it.item.uri == targetUri
        }
        val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()

        if (gridPosition > lastVisiblePosition) {
            val fab = binding.fabContinue
            fab.visibility = View.VISIBLE
            fab.isEnabled = true
            fab.setText(R.string.fab_continue)
            fab.backgroundTintList = ColorStateList.valueOf(getColor(R.color.ink))
            fab.setTextColor(getColor(android.R.color.white))
            fab.iconTint = ColorStateList.valueOf(getColor(android.R.color.white))
            fab.strokeColor = ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)
            fab.elevation = 6.dp().toFloat()
        } else {
            binding.fabContinue.visibility = View.GONE
        }
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
                    val items = uiState.displayedItems

                    // Update N LEFT counter (top bar, right of chips)
                    val unreviewedCount = items.count { it.uri !in viewedItems }
                    binding.unreviewedCounter.visibility =
                        if (unreviewedCount > 0) View.VISIBLE else View.GONE
                    binding.unreviewedCounter.text =
                        getString(R.string.n_left_counter, unreviewedCount)

                    // Refresh Continue FAB (updateContinueFabState owns visibility + state)
                    val layoutManager = binding.recyclerView.layoutManager as? GridLayoutManager
                    layoutManager?.let { updateContinueFabState(it) }

                    // Show the hint only the first time the FAB actually appears
                    if (binding.fabContinue.visibility == View.VISIBLE) {
                        hintManager.showHint(
                            HintPreferences.HINT_CONTINUE_FAB,
                            getString(R.string.hint_continue_fab_title),
                            getString(R.string.hint_continue_fab_detail),
                        )
                    }

                    // Update review progress bar
                    val totalItems = items.size
                    val viewedCount = if (totalItems > 0) {
                        items.count { it.uri in viewedItems }
                    } else 0

                    if (totalItems > 0 && viewedCount > 0 &&
                        (uiState is GalleryUiState.Normal || uiState is GalleryUiState.Selection)) {
                        binding.reviewProgressBar.visibility = View.VISIBLE
                        binding.reviewProgressBar.max = totalItems
                        binding.reviewProgressBar.setProgressCompat(viewedCount, true)
                        hintManager.showHint(
                            HintPreferences.HINT_PROGRESS_BAR,
                            getString(R.string.hint_progress_bar_title),
                            getString(R.string.hint_progress_bar_detail),
                        )
                    } else {
                        binding.reviewProgressBar.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun observeActiveFilters() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.hasActiveFilters.collect { isActive ->
                    updateFiltersButtonAppearance(isActive)
                }
            }
        }
    }

    private fun updateFiltersButtonAppearance(isActive: Boolean) {
        binding.btnFilters.isActivated = isActive
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
        // Only dismiss hints when the state TYPE changes (e.g., Normal → Selection)
        // Not on Normal → Normal updates (which happen frequently from filter/scroll events)
        val currentStateType = state::class.simpleName
        if (previousStateType != null && previousStateType != currentStateType) {
            hintManager.dismiss()
        }
        previousStateType = currentStateType

        when (state) {
            is GalleryUiState.Loading -> {
                binding.progressBar.visibility = View.VISIBLE
                binding.filterLoadingIndicator.visibility = View.GONE
                binding.filterSummary.visibility = View.GONE
                binding.reviewProgressBar.visibility = View.GONE
                binding.emptyStateContainer.visibility = View.GONE
                binding.recyclerView.visibility = View.GONE
                binding.topBar.visibility = View.GONE

                binding.selectionActionBar.visibility = View.GONE
                backCallback.isEnabled = false
            }

            is GalleryUiState.Empty -> {
                binding.progressBar.visibility = View.GONE
                binding.filterLoadingIndicator.visibility = View.GONE
                binding.filterSummary.visibility = View.GONE
                binding.reviewProgressBar.visibility = View.GONE
                binding.emptyStateContainer.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
                binding.topBar.visibility = View.GONE

                binding.selectionActionBar.visibility = View.GONE
                backCallback.isEnabled = false

                // Show generic empty state
                binding.emptyIcon.setImageResource(R.drawable.ic_search_off)
                binding.emptyTitle.text = state.message
                binding.emptySubtitle.visibility = View.GONE
                binding.btnResetFilters.visibility = View.GONE
                binding.btnEditFilters.visibility = View.GONE
            }

            is GalleryUiState.NoFiltersSelected -> {
                binding.progressBar.visibility = View.GONE
                binding.filterLoadingIndicator.visibility = View.GONE
                binding.reviewProgressBar.visibility = View.GONE
                binding.emptyStateContainer.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
                binding.topBar.visibility = View.VISIBLE

                binding.selectionActionBar.visibility = View.GONE
                backCallback.isEnabled = false

                // Show "no filters selected" state
                binding.emptyIcon.setImageResource(R.drawable.ic_search_off)
                binding.emptyTitle.text = getString(R.string.no_filters_selected)
                binding.emptySubtitle.text = getString(R.string.select_categories_hint)
                binding.emptySubtitle.visibility = View.VISIBLE
                binding.btnResetFilters.visibility = View.VISIBLE
                binding.btnEditFilters.visibility = View.GONE

                adapter.submitList(emptyList())
                updateMediaTypeChips(state.selectedMediaTypes)

            }

            is GalleryUiState.NoMatchingItems -> {
                binding.progressBar.visibility = View.GONE
                binding.filterLoadingIndicator.visibility = View.GONE
                binding.reviewProgressBar.visibility = View.GONE
                binding.emptyStateContainer.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
                binding.topBar.visibility = View.VISIBLE

                binding.selectionActionBar.visibility = View.GONE
                backCallback.isEnabled = false

                // Show "no matching items" state with the active filter combo
                val combo = FilterComboFormatter.format(
                    context = this,
                    mediaTypes = state.selectedMediaTypes,
                    sources = state.selectedSources,
                    allAvailableSources = state.allSourceCounts.keys,
                    dateRange = state.selectedDateRange,
                    sortOption = state.selectedSortOption,
                )
                binding.emptyIcon.setImageResource(R.drawable.ic_search_off)
                binding.emptyTitle.text = getString(R.string.no_matching_items)
                binding.emptySubtitle.text = getString(R.string.no_matches_subtitle, combo)
                binding.emptySubtitle.visibility = View.VISIBLE
                binding.btnResetFilters.visibility = View.VISIBLE
                binding.btnEditFilters.visibility = View.VISIBLE

                adapter.submitList(emptyList())
                updateMediaTypeChips(state.selectedMediaTypes)

            }

            is GalleryUiState.Normal -> {
                binding.progressBar.visibility = View.GONE
                binding.emptyStateContainer.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
                binding.topBar.visibility = View.VISIBLE

                binding.selectionActionBar.visibility = View.GONE
                dragSelectListener.inSelectionMode = false

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

                adapter.submitList(DateBucket.bucketize(state.items)) {
                    binding.filterLoadingIndicator.visibility = View.GONE
                    if (filtersChanged) {
                        binding.recyclerView.scrollToPosition(0)
                    }
                }
                adapter.updateSelectionState(emptySet(), false)
                updateMediaTypeChips(state.selectedMediaTypes)

                backCallback.isEnabled = false

                // Show filters hint on first load with items
                hintManager.showHint(
                    HintPreferences.HINT_FILTERS,
                    getString(R.string.hint_filters_title),
                    getString(R.string.hint_filters_detail),
                )
                hintManager.showHint(
                    HintPreferences.HINT_PINCH_ZOOM,
                    getString(R.string.hint_pinch_zoom_title),
                    getString(R.string.hint_pinch_zoom_detail),
                )
            }

            is GalleryUiState.Selection -> {
                binding.progressBar.visibility = View.GONE
                binding.filterLoadingIndicator.visibility = View.GONE
                binding.emptyStateContainer.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
                binding.topBar.visibility = View.VISIBLE

                binding.selectionActionBar.visibility = View.VISIBLE
                dragSelectListener.inSelectionMode = true

                // Show selection count on top, total size below in mono
                val selectedCount = state.selectedItems.size
                val totalSize = viewModel.getSelectedItemsTotalSize()
                binding.selectionCount.text = if (state.hiddenSelectedCount > 0) {
                    getString(R.string.selected_with_hidden, selectedCount, state.hiddenSelectedCount)
                } else {
                    getString(R.string.selected_count, selectedCount)
                }
                binding.selectionSizeSmall.visibility =
                    if (totalSize > 0L) View.VISIBLE else View.GONE
                binding.selectionSizeSmall.text = Formatter.formatFileSize(this, totalSize)

                adapter.submitList(DateBucket.bucketize(state.items))
                adapter.updateSelectionState(state.selectedItems, true)
                updateMediaTypeChips(state.selectedMediaTypes)

                backCallback.isEnabled = true
            }
        }
    }

    private fun handleItemClick(item: MediaItem) {
        val state = viewModel.uiState.value
        if (state is GalleryUiState.Selection) {
            viewModel.toggleItemSelection(item.uri)
        } else {
            hintManager.showHint(
                HintPreferences.HINT_LONG_PRESS,
                getString(R.string.hint_long_press_title),
                getString(R.string.hint_long_press_detail),
            )
            openMediaViewer(item)
        }
    }

    private fun openMediaViewer(item: MediaItem) {
        val intent = Intent(this, MediaViewerActivity::class.java).apply {
            putExtra(MediaViewerActivity.EXTRA_URI, item.uri)
            putExtra(MediaViewerActivity.EXTRA_MEDIA_TYPE, item.mediaType.ordinal)
            putExtra(MediaViewerActivity.EXTRA_DISPLAY_NAME, item.displayName)
            putExtra(MediaViewerActivity.EXTRA_DATE_ADDED, item.dateAdded)
            putExtra(MediaViewerActivity.EXTRA_SIZE, item.size)
            putExtra(MediaViewerActivity.EXTRA_SOURCE, item.source.ordinal)
        }
        mediaViewerLauncher.launch(intent)
    }

    private fun handleItemLongClick(item: MediaItem) {
        val state = viewModel.uiState.value
        if (state !is GalleryUiState.Selection) {
            viewModel.enterSelectionMode(item.uri)
            // Snapshot the selection (contains the initial item) for drag range math
            preDragSelection = viewModel.getSelectedItems()

            val position = adapter.currentList.indexOfFirst {
                it is GridItem.Media && it.item.uri == item.uri
            }
            if (position >= 0) {
                dragSelectListener.startDragSelection(position)
            }

            hintManager.showHint(
                HintPreferences.HINT_DRAG_SELECT,
                getString(R.string.hint_drag_select_title),
                getString(R.string.hint_drag_select_detail),
            )
        }
    }

    private fun handleDelete() {
        val selectedItems = viewModel.getSelectedItems()
        if (selectedItems.isEmpty()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ — skip custom dialog, system trash dialog provides confirmation
            performTrash(selectedItems)
        } else {
            // Pre-Android 11 — keep confirmation since delete is irreversible
            val state = viewModel.uiState.value
            val hiddenCount = if (state is GalleryUiState.Selection) state.hiddenSelectedCount else 0
            showTrashConfirmationDialog(selectedItems.size, hiddenCount) {
                performTrash(selectedItems)
            }
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
        pendingTrashSize = viewModel.getTotalSize(uris)
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
                showSnackbar(getString(R.string.trash_success_with_size, deletedCount, Formatter.formatFileSize(this, totalSize)))
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

    private var undoAnimator: android.animation.ValueAnimator? = null

    private fun showTrashSuccessCard(count: Int, totalSize: Long, trashedUris: Set<Uri>) {
        binding.deleteSuccessOverlay.visibility = View.VISIBLE
        binding.deleteSuccessOverlay.bringToFront()
        window.statusBarColor = getColor(R.color.overlay_bg)
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = false

        binding.successHeroSize.text = Formatter.formatFileSize(this, totalSize)
        binding.successSubtitle.text = if (count == 1) {
            getString(R.string.freed_subtitle_single)
        } else {
            getString(R.string.freed_subtitle, count)
        }

        binding.confettiLayer.start(System.nanoTime())
        binding.undoProgressRing.progress = 1f

        binding.deleteSuccessOverlay.alpha = 0f
        binding.deleteSuccessOverlay.animate()
            .alpha(1f)
            .setDuration(300)
            .start()

        undoAnimator?.cancel()
        undoAnimator = android.animation.ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 7000
            interpolator = android.view.animation.LinearInterpolator()
            addUpdateListener { binding.undoProgressRing.progress = it.animatedValue as Float }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    dismissSuccessOverlay()
                }
            })
            start()
        }

        binding.btnOverlayUndo.setOnClickListener {
            undoAnimator?.cancel()
            performRestore(trashedUris)
            dismissSuccessOverlay()
        }
        binding.btnOverlayContinue.setOnClickListener {
            undoAnimator?.cancel()
            dismissSuccessOverlay()
        }

        // Scrim consumes taps but does not dismiss.
        binding.deleteSuccessOverlay.setOnClickListener(null)
        binding.deleteSuccessOverlay.isClickable = true

        hintManager.showHint(
            HintPreferences.HINT_TRASH_UNDO,
            getString(R.string.hint_trash_undo_title),
            getString(R.string.hint_trash_undo_detail),
        )
    }

    private fun dismissSuccessOverlay() {
        window.statusBarColor = getColor(android.R.color.white)
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = true

        undoAnimator?.cancel()
        undoAnimator = null

        binding.deleteSuccessOverlay.animate().cancel()

        binding.deleteSuccessOverlay.animate()
            .alpha(0f)
            .setDuration(200)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    binding.deleteSuccessOverlay.visibility = View.GONE
                    binding.confettiLayer.stop()
                    binding.deleteSuccessOverlay.animate().setListener(null)
                }
            })
            .start()
    }

    private fun changeGridColumns(layoutManager: GridLayoutManager, newSpanCount: Int) {
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        layoutManager.spanCount = newSpanCount
        if (firstVisible != RecyclerView.NO_POSITION) {
            layoutManager.scrollToPosition(firstVisible)
        }
        adapter.thumbnailSize = resources.displayMetrics.widthPixels / newSpanCount
        viewModel.setGridColumnCount(newSpanCount)
    }

    private fun formatDateForPosition(position: Int): String {
        val items = adapter.currentList
        if (position !in items.indices) return ""
        val mediaItem = (items[position] as? GridItem.Media)?.item ?: return ""

        val timestamp = mediaItem.dateAdded
        val itemDate = Date(timestamp * 1000L)
        val now = Calendar.getInstance()
        val itemCal = Calendar.getInstance().apply { time = itemDate }

        return when {
            now.get(Calendar.YEAR) == itemCal.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == itemCal.get(Calendar.DAY_OF_YEAR) -> "Today"

            now.get(Calendar.YEAR) == itemCal.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) - itemCal.get(Calendar.DAY_OF_YEAR) == 1 -> "Yesterday"

            now.get(Calendar.YEAR) == itemCal.get(Calendar.YEAR) ->
                SimpleDateFormat("MMM d", Locale.getDefault()).format(itemDate)

            else ->
                SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(itemDate)
        }
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        binding.emptyStateContainer.visibility = View.VISIBLE
        binding.emptyIcon.setImageResource(R.drawable.ic_search_off)
        binding.emptyTitle.text = message
        binding.emptySubtitle.visibility = View.GONE
        binding.btnResetFilters.visibility = View.GONE
        binding.btnEditFilters.visibility = View.GONE
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
