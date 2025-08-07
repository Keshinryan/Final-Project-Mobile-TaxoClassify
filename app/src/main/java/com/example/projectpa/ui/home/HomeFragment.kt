package com.example.projectpa.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.projectpa.PredictionManager
import com.example.projectpa.R
import com.example.projectpa.databinding.FragmentHomeBinding
import com.example.projectpa.ui.result.ResultFragment
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var currentPhotoPath: String
    private lateinit var imageUri: Uri

    // Launcher to request camera permission
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            dispatchTakePictureIntent()
        } else {
            Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // Camera launcher
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            navigateToResultFragment(imageUri)
        }
    }

    // Gallery launcher
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            navigateToResultFragment(it)
        }
    }
    private fun navigateToResultFragment(imageUri: Uri) {
        lifecycleScope.launch {
            val result = PredictionManager.predict(requireContext(), imageUri)
            val bundle = Bundle().apply {
                result.forEach { (key, pair) ->
                    putString(key, pair.first)  // label
                    putFloat("${key}_confidence", pair.second)  // confidence
                }
                putString("image_uri", imageUri.toString())
            }
            findNavController().navigate(R.id.action_home_to_result, bundle)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        // ViewModel example (optional)
        val textView: TextView = binding.textHome
        val homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        homeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        binding.captureBtn.setOnClickListener {
            if (hasCameraPermission()) {
                dispatchTakePictureIntent()
            } else {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        binding.galleryBtn.setOnClickListener {
            openGallery()
        }

        return binding.root
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun dispatchTakePictureIntent() {
        val photoFile: File? = try {
            createImageFile()
        } catch (ex: IOException) {
            Toast.makeText(requireContext(), "Error creating file", Toast.LENGTH_SHORT).show()
            null
        }

        photoFile?.also {
            imageUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                it
            )
            cameraLauncher.launch(imageUri)
        }
    }

    private fun createImageFile(): File {
        val timeStamp: String =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? =
            requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", ".jpg", storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
