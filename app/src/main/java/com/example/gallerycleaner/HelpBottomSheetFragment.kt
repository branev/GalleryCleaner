package com.example.gallerycleaner

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.gallerycleaner.databinding.BottomSheetHelpBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar

class HelpBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetHelpBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = BottomSheetHelpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTips()
        setupResetButton()
        setupKofiCard()
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

    private fun setupTips() {
        val tips = listOf(
            R.drawable.ic_touch_app to R.string.tip_long_press,
            R.drawable.ic_swipe to R.string.tip_drag_select,
            R.drawable.ic_pinch_zoom to R.string.tip_pinch_zoom,
            R.drawable.ic_filter_list to R.string.tip_filters,
            R.drawable.ic_arrow_downward to R.string.tip_fast_scroll,
            R.drawable.ic_arrow_downward to R.string.tip_continue,
            R.drawable.ic_delete to R.string.tip_trash,
            R.drawable.ic_undo to R.string.tip_undo,
        )

        val ctx = requireContext()
        val accent = ContextCompat.getColor(ctx, R.color.accent)
        val inkSecondary = ContextCompat.getColor(ctx, R.color.ink2)
        val lineColor = ContextCompat.getColor(ctx, R.color.line)

        tips.forEachIndexed { index, (iconRes, textRes) ->
            if (index > 0) {
                val divider = View(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
                    )
                    setBackgroundColor(lineColor)
                }
                binding.tipContainer.addView(divider)
            }

            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, dp(12), 0, dp(12))
            }

            val icon = ImageView(ctx).apply {
                setImageResource(iconRes)
                setColorFilter(accent)
                layoutParams = LinearLayout.LayoutParams(dp(22), dp(22)).apply {
                    marginEnd = dp(12)
                }
            }

            val text = TextView(ctx).apply {
                setText(textRes)
                textSize = 13f
                setTextColor(inkSecondary)
            }

            row.addView(icon)
            row.addView(text)
            binding.tipContainer.addView(row)
        }
    }

    private fun setupResetButton() {
        binding.btnResetTips.setOnClickListener {
            HintPreferences(requireContext()).resetAllHints()
            Snackbar.make(
                requireView(),
                getString(R.string.hints_reset_confirmation),
                Snackbar.LENGTH_SHORT,
            ).show()
        }
    }

    private fun setupKofiCard() {
        binding.kofiCard.setOnClickListener {
            startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://ko-fi.com/branev"))
            )
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        const val TAG = "HelpBottomSheet"
        fun newInstance() = HelpBottomSheetFragment()
    }
}
