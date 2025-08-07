package com.example.projectpa.ui.help
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.projectpa.R
import com.example.projectpa.databinding.FragmentHelpBinding

class HelpFragment : Fragment() {

    private var _binding: FragmentHelpBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHelpBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textHelp
        textView.text = Html.fromHtml(getString(R.string.app_instructions), Html.FROM_HTML_MODE_COMPACT)
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}