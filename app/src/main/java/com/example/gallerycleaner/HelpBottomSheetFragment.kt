package com.example.gallerycleaner

import android.content.Intent
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.gallerycleaner.databinding.BottomSheetHelpBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar

class HelpBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetHelpBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = BottomSheetHelpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTips()
        setupSupportButton()
        setupResetButton()
    }

    private fun setupTips() {
        val tips = listOf(
            Pair(R.drawable.ic_touch_app, R.string.tip_long_press),
            Pair(R.drawable.ic_swipe, R.string.tip_drag_select),
            Pair(R.drawable.ic_pinch_zoom, R.string.tip_pinch_zoom),
            Pair(R.drawable.ic_filter_list, R.string.tip_filters),
            Pair(R.drawable.ic_arrow_downward, R.string.tip_fast_scroll),
            Pair(R.drawable.ic_arrow_downward, R.string.tip_continue),
            Pair(R.drawable.ic_delete, R.string.tip_trash),
            Pair(R.drawable.ic_undo, R.string.tip_undo)
        )

        for ((iconRes, textRes) in tips) {
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, dp(8), 0, dp(8))
            }

            val icon = ImageView(requireContext()).apply {
                setImageResource(iconRes)
                setColorFilter(requireContext().getColor(R.color.fast_scroll_tooltip_bg))
                layoutParams = LinearLayout.LayoutParams(dp(24), dp(24)).apply {
                    marginEnd = dp(16)
                }
            }

            val text = TextView(requireContext()).apply {
                setText(textRes)
                textSize = 15f
                setTextColor(requireContext().getColor(R.color.md_theme_onSurface))
            }

            row.addView(icon)
            row.addView(text)
            binding.tipContainer.addView(row)
        }
    }

    private fun setupSupportButton() {
        binding.btnSupportDeveloper.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://ko-fi.com/branev")))
        }
    }

    private fun setupResetButton() {
        binding.btnOkHelp.setOnClickListener {
            dismiss()
        }
        binding.btnResetTips.paintFlags = binding.btnResetTips.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        binding.btnResetTips.setOnClickListener {
            HintPreferences(requireContext()).resetAllHints()
            Snackbar.make(requireView(), getString(R.string.hints_reset_confirmation), Snackbar.LENGTH_SHORT).show()
            dismiss()
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    companion object {
        const val TAG = "HelpBottomSheet"
        fun newInstance() = HelpBottomSheetFragment()
    }
}
