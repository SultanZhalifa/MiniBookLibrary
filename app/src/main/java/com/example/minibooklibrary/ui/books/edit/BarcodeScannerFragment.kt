package com.example.minibooklibrary.ui.books.edit

import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.example.minibooklibrary.R
import com.example.minibooklibrary.databinding.FragmentBarcodeScannerBinding
import com.example.minibooklibrary.ui.common.applySystemBarInsets
import com.example.minibooklibrary.ui.common.toast
import com.example.minibooklibrary.util.Isbn
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class BarcodeScannerFragment : Fragment() {

    private var _binding: FragmentBarcodeScannerBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner
    private val resultDelivered = AtomicBoolean(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera()
        else {
            toast(R.string.scanner_permission_denied)
            findNavController().popBackStack()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBarcodeScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        barcodeScanner = BarcodeScanning.getClient()
        binding.btnClose.setOnClickListener { findNavController().popBackStack() }
        binding.topBar.applySystemBarInsets(top = true)

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            // 720p is plenty for a barcode and keeps per-frame analysis cheap.
            val resolution = ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        Size(1280, 720),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                    )
                )
                .build()
            val imageAnalysis = ImageAnalysis.Builder()
                .setResolutionSelector(resolution)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(cameraExecutor, ::analyzeImage) }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
            } catch (_: Exception) {
                findNavController().popBackStack()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    // Opt in here instead of re-declaring the marker, which would spread the experimental
    // requirement to every caller (setAnalyzer).
    @OptIn(ExperimentalGetImage::class)
    private fun analyzeImage(imageProxy: androidx.camera.core.ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                // Books print their ISBN as an EAN-13. The checksum and 978/979 prefix check
                // in Isbn rules out every other product barcode in the frame.
                val isbn = barcodes.asSequence()
                    .filter { it.format == Barcode.FORMAT_EAN_13 }
                    .firstNotNullOfOrNull { Isbn.normalize(it.rawValue) }
                if (isbn != null && resultDelivered.compareAndSet(false, true)) {
                    deliverResult(isbn)
                }
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    /** Runs on the main thread (ML Kit's default listener executor). */
    private fun deliverResult(isbn: String) {
        // A frame can finish processing after the user already closed the scanner.
        if (_binding == null) return
        setFragmentResult(REQUEST_KEY, Bundle().apply { putString(KEY_ISBN, isbn) })
        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        barcodeScanner.close()
        _binding = null
    }

    companion object {
        const val REQUEST_KEY = "barcode_scanner_result"
        const val KEY_ISBN = "isbn"
    }
}
