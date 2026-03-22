package com.example.gallerycleaner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import com.example.gallerycleaner.databinding.BottomSheetFiltersBinding
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

        setupResetButton()
        setupDateRangeChips()
        setupSourceChips()
        setupSortChips()
        observeActiveFilters()
    }

    private fun setupResetButton() {
        binding.btnReset.setOnClickListener {
            viewModel.resetFilters()
            // Refresh chips
            setupDateRangeChips()
            setupSourceChips()
            setupSortChips()
        }
    }

    private fun observeActiveFilters() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.hasActiveFilters.collect { isActive ->
                    binding.btnReset.isEnabled = isActive
                    binding.btnReset.alpha = if (isActive) 1.0f else 0.4f
                }
            }
        }
    }

    private fun setupDateRangeChips() {
        binding.dateRangeChipGroup.removeAllViews()
        val selectedDateRange = viewModel.selectedDateRange.value

        DateRangePreset.values().forEach { preset ->
            val chip = Chip(requireContext()).apply {
                text = when {
                    preset == DateRangePreset.CUSTOM && selectedDateRange.preset == DateRangePreset.CUSTOM -> {
                        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
                        val startDate = Date((selectedDateRange.startTimestamp ?: 0L) * 1000)
                        val endDate = Date((selectedDateRange.endTimestamp ?: System.currentTimeMillis() / 1000) * 1000)
                        "${dateFormat.format(startDate)} - ${dateFormat.format(endDate)}"
                    }
                    else -> preset.label
                }
                isCheckable = true
                isChecked = preset == selectedDateRange.preset
                isCheckedIconVisible = true
                setOnClickListener {
                    if (preset == DateRangePreset.CUSTOM) {
                        showDateRangePicker()
                    } else {
                        viewModel.setDateRangePreset(preset)
                        setupDateRangeChips() // Refresh to update selection
                        setupSourceChips() // Refresh source counts for new date range
                    }
                }
            }
            binding.dateRangeChipGroup.addView(chip)
        }
    }

    private fun setupSourceChips() {
        binding.sourceChipGroup.removeAllViews()
        val selectedSources = viewModel.selectedSources.value

        val state = viewModel.uiState.value
        // Use sourceCounts (filtered by date range) so counts reflect the current date filter
        val sourceCounts = when (state) {
            is GalleryUiState.Normal -> state.sourceCounts
            is GalleryUiState.Selection -> state.sourceCounts
            is GalleryUiState.NoFiltersSelected -> state.sourceCounts
            is GalleryUiState.NoMatchingItems -> state.sourceCounts
            else -> emptyMap()
        }
        // Use allSourceCounts to know which sources exist (for "All" chip logic)
        val allSourceCounts = when (state) {
            is GalleryUiState.Normal -> state.allSourceCounts
            is GalleryUiState.Selection -> state.allSourceCounts
            is GalleryUiState.NoFiltersSelected -> state.allSourceCounts
            is GalleryUiState.NoMatchingItems -> state.allSourceCounts
            else -> emptyMap()
        }

        // "All" chip - uses allSourceCounts to determine all available sources
        val totalCount = sourceCounts.values.sum()
        val allAvailableSources = allSourceCounts.keys
        val allSelected = allAvailableSources.isNotEmpty() && selectedSources.containsAll(allAvailableSources)

        val allChip = Chip(requireContext()).apply {
            text = getString(R.string.all_with_count, totalCount)
            isCheckable = true
            isChecked = allSelected
            isCheckedIconVisible = true
            setOnClickListener {
                viewModel.toggleAllSources()
                setupSourceChips() // Refresh to update selection
            }
        }
        binding.sourceChipGroup.addView(allChip)

        // Source chips
        SourceType.values().forEach { source ->
            val count = sourceCounts[source] ?: 0
            if (count > 0) {
                val chip = Chip(requireContext()).apply {
                    text = "${source.label} ($count)"
                    isCheckable = true
                    isChecked = source in selectedSources
                    isCheckedIconVisible = true
                    setOnClickListener {
                        viewModel.toggleSourceFilter(source)
                        setupSourceChips() // Refresh to update selection
                    }
                }
                binding.sourceChipGroup.addView(chip)
            }
        }
    }

    private fun setupSortChips() {
        binding.sortChipGroup.removeAllViews()
        val selectedSort = viewModel.selectedSortOption.value

        SortOption.values().forEach { sortOption ->
            val chip = Chip(requireContext()).apply {
                text = sortOption.label
                isCheckable = true
                isChecked = sortOption == selectedSort
                isCheckedIconVisible = true
                setOnClickListener {
                    viewModel.setSortOption(sortOption)
                    setupSortChips() // Refresh to update selection
                }
            }
            binding.sortChipGroup.addView(chip)
        }
    }

    private fun showDateRangePicker() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(getString(R.string.select_date_range))
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            viewModel.setCustomDateRange(selection.first, selection.second)
            setupDateRangeChips() // Refresh to show custom range
            setupSourceChips() // Refresh source counts for new date range
        }

        picker.show(parentFragmentManager, "date_picker")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "FilterBottomSheetFragment"

        fun newInstance(): FilterBottomSheetFragment {
            return FilterBottomSheetFragment()
        }
    }
}
