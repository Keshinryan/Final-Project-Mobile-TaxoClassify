package com.example.projectpa.ui.result
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.projectpa.databinding.FragmentResultBinding
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan

class ResultFragment : Fragment() {

    private var _binding: FragmentResultBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultBinding.inflate(inflater, container, false)
        val view = binding.root

        // Get prediction values from bundle
        val imageUriString = arguments?.getString("image_uri")
        val kelas = arguments?.getString("kelas")
        val ordo = arguments?.getString("ordo")
        val famili = arguments?.getString("famili")
        val genus = arguments?.getString("genus")
        val spesies = arguments?.getString("spesies")

        imageUriString?.let {
            val uri = Uri.parse(it)
            Glide.with(this)
                .load(uri)
                .into(binding.imageResult) // Use ViewBinding
        }
        val keys = listOf("kelas", "ordo", "famili", "genus", "spesies")
        val views = listOf(
            binding.classOutput,
            binding.ordoOutput,
            binding.familiOutput,
            binding.genusOutput,
            binding.spesiesOutput
        )

        for (i in keys.indices) {
            val label = arguments?.getString(keys[i]) ?: "-"
            val confidence = arguments?.getFloat("${keys[i]}_confidence", 0f) ?: 0f
            val confidencePercentage = (confidence * 100).toInt()

            val (statusText, statusColor) = if (confidence >= 0.7f) {
                "Yakin" to android.graphics.Color.parseColor("#74B4AE") // green
            } else {
                "Tidak Yakin" to android.graphics.Color.parseColor("#D77C7C") // red
            }

            val spannable = SpannableStringBuilder()

            if (keys[i] == "spesies" && label.contains(" ")) {
                val scientificEnd = label.indexOf(" (")
                val scientificName = if (scientificEnd != -1) label.substring(0, scientificEnd) else label
                val vernacularName = if (scientificEnd != -1) label.substring(scientificEnd) else ""

                // Append and italicize scientific name only
                val italicStart = spannable.length
                spannable.append(scientificName)
                spannable.setSpan(
                    android.text.style.StyleSpan(android.graphics.Typeface.ITALIC),
                    italicStart,
                    spannable.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                spannable.append(vernacularName)
            } else {
                spannable.append(label)
            }

// Append confidence info (always colored)
            val statusWithConfidence = " ($statusText, $confidencePercentage%)"
            val colorStart = spannable.length
            spannable.append(statusWithConfidence)
            spannable.setSpan(
                ForegroundColorSpan(statusColor),
                colorStart,
                spannable.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

// Set to corresponding TextView
            views[i].text = spannable

        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}