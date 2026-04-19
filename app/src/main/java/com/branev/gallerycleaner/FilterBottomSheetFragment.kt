package com.branev.gallerycleaner

import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.activityViewModels
import com.branev.gallerycleaner.databinding.BottomSheetFiltersBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FilterBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetFiltersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GalleryViewModel by activityViewModels()

    // Pending state — initialized from viewModel on onViewCreated
    private lateinit var pendingMediaTypes: MutableSet<MediaType>
    private lateinit var pendingSources: MutableSet<SourceType>
    private var pendingDateRange: DateRange = DateRange.DEFAULT
    private lateinit var pendingSortOption: SortOption

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetFiltersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pendingMediaTypes = viewModel.selectedMediaTypes.value.toMutableSet()
        pendingSources = viewModel.selectedSources.value.toMutableSet()
        pendingDateRange = viewModel.selectedDateRange.value
        pendingSortOption = viewModel.selectedSortOption.value

        setSectionLabels()
        setupTypeChips()
        setupSourceChips()
        setupDateRangeChips()
        setupSortChips()
        setupFooter()
        setupReset()
        refreshApplyButton()
    }

    override fun onStart() {
        super.onStart()
        // Light status-bar icons while the sheet's dark scrim is showing.
        dialog?.window?.let { w ->
            WindowCompat.getInsetsController(w, w.decorView)
                .isAppearanceLightStatusBars = false
        }
    }

    override fun onDestroyView() {
        // Restore dark status-bar icons for the main activity.
        requireActivity().window.let { w ->
            WindowCompat.getInsetsController(w, w.decorView)
                .isAppearanceLightStatusBars = true
        }
        super.onDestroyView()
        _binding = null
    }

    private fun setSectionLabels() {
        binding.labelType.root.setText(R.string.filter_section_type)
        binding.labelSource.root.setText(R.string.filter_section_source)
        binding.labelDate.root.setText(R.string.filter_section_date)
        binding.labelSort.root.setText(R.string.filter_section_sort)
    }

    /** Inflates a styled chip (PillFilterChip) from the item layout. */
    private fun makeChip(parent: ViewGroup): Chip {
        val chip = layoutInflater.inflate(R.layout.item_filter_chip, parent, false) as Chip
        val ctx = requireContext()
        chip.chipBackgroundColor = AppCompatResources.getColorStateList(ctx, R.color.chip_bg_color)
        chip.chipStrokeColor = AppCompatResources.getColorStateList(ctx, R.color.chip_stroke_color)
        chip.setTextColor(AppCompatResources.getColorStateList(ctx, R.color.chip_text_color))
        chip.rippleColor = android.content.res.ColorStateList.valueOf(
            ContextCompat.getColor(ctx, android.R.color.transparent)
        )
        return chip
    }

    private fun setupTypeChips() {
        binding.typeChipGroup.removeAllViews()
        MediaType.values().forEach { type ->
            val label = if (type == MediaType.PHOTO)
                getString(R.string.photos) else getString(R.string.videos)
            val chip = makeChip(binding.typeChipGroup).apply {
                text = label
                isCheckable = true
                isChecked = type in pendingMediaTypes
                setOnClickListener {
                    if (isChecked) pendingMediaTypes.add(type) else pendingMediaTypes.remove(type)
                    refreshApplyButton()
                }
            }
            binding.typeChipGroup.addView(chip)
        }
    }

    private fun setupSourceChips() {
        binding.sourceChipGroup.removeAllViews()
        val state = viewModel.uiState.value
        val sourceCounts = when (state) {
            is GalleryUiState.Normal -> state.sourceCounts
            is GalleryUiState.Selection -> state.sourceCounts
            is GalleryUiState.NoFiltersSelected -> state.sourceCounts
            is GalleryUiState.NoMatchingItems -> state.sourceCounts
            else -> emptyMap()
        }
        val allSourceCounts = when (state) {
            is GalleryUiState.Normal -> state.allSourceCounts
            is GalleryUiState.Selection -> state.allSourceCounts
            is GalleryUiState.NoFiltersSelected -> state.allSourceCounts
            is GalleryUiState.NoMatchingItems -> state.allSourceCounts
            else -> emptyMap()
        }

        // "All" chip
        val totalCount = sourceCounts.values.sum()
        val allAvailableSources = allSourceCounts.keys
        val allSelected = allAvailableSources.isNotEmpty() &&
                pendingSources.containsAll(allAvailableSources)
        val allChip = makeChip(binding.sourceChipGroup).apply {
            text = sourceChipLabel(getString(R.string.filter_type_all), totalCount, allSelected)
            isCheckable = true
            isChecked = allSelected
            setOnClickListener {
                if (isChecked) pendingSources.addAll(allAvailableSources)
                else pendingSources.clear()
                setupSourceChips() // Refresh to sync per-source chips
                refreshApplyButton()
            }
        }
        binding.sourceChipGroup.addView(allChip)

        // Per-source chips
        SourceType.values().forEach { source ->
            val count = sourceCounts[source] ?: 0
            if (count > 0) {
                val selected = source in pendingSources
                val chip = makeChip(binding.sourceChipGroup).apply {
                    text = sourceChipLabel(source.label, count, selected)
                    isCheckable = true
                    isChecked = selected
                    setOnClickListener {
                        if (isChecked) pendingSources.add(source)
                        else pendingSources.remove(source)
                        refreshApplyButton()
                    }
                }
                binding.sourceChipGroup.addView(chip)
            }
        }
    }

    /**
     * Builds a SpannableString like "WhatsApp  5" where "  5" is in JetBrains Mono
     * at a muted color so the count reads as secondary information.
     */
    private fun sourceChipLabel(name: String, count: Int, isChecked: Boolean): CharSequence {
        val text = "$name  $count"
        val spannable = SpannableString(text)
        val start = name.length
        val end = text.length

        val monoTypeface: Typeface? =
            ResourcesCompat.getFont(requireContext(), R.font.jetbrains_mono)
        if (monoTypeface != null) {
            spannable.setSpan(
                CustomTypefaceSpan(monoTypeface),
                start, end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        // Muted count color — white@70% on the ink-filled active chip,
        // ink3 on the outlined inactive chip.
        val countColor = if (isChecked) {
            0xB3FFFFFF.toInt()
        } else {
            ContextCompat.getColor(requireContext(), R.color.ink3)
        }
        spannable.setSpan(
            ForegroundColorSpan(countColor),
            start, end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return spannable
    }

    private fun setupDateRangeChips() {
        binding.dateRangeChipGroup.removeAllViews()
        DateRangePreset.values().forEach { preset ->
            val chip = makeChip(binding.dateRangeChipGroup).apply {
                text = when {
                    preset == DateRangePreset.CUSTOM &&
                            pendingDateRange.preset == DateRangePreset.CUSTOM -> {
                        val fmt = SimpleDateFormat("MMM d", Locale.getDefault())
                        val start = Date((pendingDateRange.startTimestamp ?: 0L) * 1000)
                        val end = Date(
                            (pendingDateRange.endTimestamp
                                ?: System.currentTimeMillis() / 1000) * 1000
                        )
                        "${fmt.format(start)} – ${fmt.format(end)}"
                    }
                    else -> preset.label
                }
                isCheckable = true
                isChecked = preset == pendingDateRange.preset
                setOnClickListener {
                    if (preset == DateRangePreset.CUSTOM) {
                        showDateRangePicker()
                    } else {
                        pendingDateRange = DateRange(preset)
                        setupDateRangeChips()
                        refreshApplyButton()
                    }
                }
            }
            binding.dateRangeChipGroup.addView(chip)
        }
    }

    private fun setupSortChips() {
        binding.sortChipGroup.removeAllViews()
        SortOption.values().forEach { option ->
            val chip = makeChip(binding.sortChipGroup).apply {
                text = option.label
                isCheckable = true
                isChecked = option == pendingSortOption
                setOnClickListener {
                    pendingSortOption = option
                    setupSortChips()
                    refreshApplyButton()
                }
            }
            binding.sortChipGroup.addView(chip)
        }
    }

    private fun setupReset() {
        binding.btnReset.setOnClickListener {
            // Match viewModel.resetFilters() exactly so countChanges returns 0
            // when the app is already at defaults.
            pendingMediaTypes = MediaType.values().toMutableSet()
            pendingSources = SourceType.values().toMutableSet()
            pendingDateRange = DateRange.DEFAULT
            pendingSortOption = SortOption.DATE_DESC
            setupTypeChips()
            setupSourceChips()
            setupDateRangeChips()
            setupSortChips()
            refreshApplyButton()
        }
    }

    private fun setupFooter() {
        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnApply.setOnClickListener {
            commitPending()
            dismiss()
        }
    }

    private fun countChanges(): Int {
        var n = 0
        if (pendingMediaTypes != viewModel.selectedMediaTypes.value) n++
        if (pendingSources != viewModel.selectedSources.value) n++
        if (pendingDateRange != viewModel.selectedDateRange.value) n++
        if (pendingSortOption != viewModel.selectedSortOption.value) n++
        return n
    }

    private fun refreshApplyButton() {
        val changes = countChanges()
        binding.btnApply.text = if (changes > 0) {
            getString(R.string.apply_with_changes, changes)
        } else {
            getString(R.string.apply)
        }
    }

    private fun commitPending() {
        if (pendingMediaTypes != viewModel.selectedMediaTypes.value) {
            viewModel.setSelectedMediaTypes(pendingMediaTypes)
        }
        if (pendingSources != viewModel.selectedSources.value) {
            viewModel.setSelectedSources(pendingSources)
        }
        if (pendingDateRange != viewModel.selectedDateRange.value) {
            if (pendingDateRange.preset == DateRangePreset.CUSTOM) {
                viewModel.setCustomDateRange(
                    (pendingDateRange.startTimestamp ?: 0L) * 1000L,
                    (pendingDateRange.endTimestamp
                        ?: (System.currentTimeMillis() / 1000)) * 1000L
                )
            } else {
                viewModel.setDateRangePreset(pendingDateRange.preset)
            }
        }
        if (pendingSortOption != viewModel.selectedSortOption.value) {
            viewModel.setSortOption(pendingSortOption)
        }
    }

    private fun showDateRangePicker() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(getString(R.string.select_date_range))
            .build()
        picker.addOnPositiveButtonClickListener { selection ->
            pendingDateRange = DateRange(
                preset = DateRangePreset.CUSTOM,
                startTimestamp = selection.first / 1000L,
                endTimestamp = selection.second / 1000L,
            )
            setupDateRangeChips()
            refreshApplyButton()
        }
        picker.show(parentFragmentManager, "date_picker")
    }

    companion object {
        const val TAG = "FilterBottomSheetFragment"
        fun newInstance(): FilterBottomSheetFragment = FilterBottomSheetFragment()
    }
}
