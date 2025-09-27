package com.app.simon

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide

class FullscreenImageDialog : DialogFragment() {

    companion object {
        private const val ARG_URI = "uri"
        fun newInstance(uri: String): FullscreenImageDialog {
            val args = Bundle()
            args.putString(ARG_URI, uri)
            val fragment = FullscreenImageDialog()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.dialog_fullscreen_image, container, false)
        val imageView = view.findViewById<ImageView>(R.id.fullscreenImage)
        val uriString = arguments?.getString(ARG_URI)
        if (!uriString.isNullOrEmpty()) {
            val uri = Uri.parse(uriString)
            Glide.with(requireContext())
                .load(uri)
                .fitCenter()
                .into(imageView)
        } else {

        }


        val scaleGestureDetector = ScaleGestureDetector(
            requireContext(),
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                var scaleFactor = 1f
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    scaleFactor *= detector.scaleFactor
                    scaleFactor = scaleFactor.coerceIn(1.0f, 5.0f)
                    imageView.scaleX = scaleFactor
                    imageView.scaleY = scaleFactor
                    return true
                }
            }
        )


        imageView.setOnTouchListener { v, event ->
            scaleGestureDetector.onTouchEvent(event)
            true
        }

        return view
    }
}
