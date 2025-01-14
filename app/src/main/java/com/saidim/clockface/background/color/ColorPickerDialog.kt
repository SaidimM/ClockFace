package com.saidim.clockface.background.color

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.skydoves.colorpickerview.ColorPickerView
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import com.saidim.clockface.R

class ColorPickerDialog : DialogFragment() {
    private var onColorSelected: ((Int) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val colorPickerView = ColorPickerView(requireContext()).apply {
            setInitialColor(arguments?.getInt(ARG_INITIAL_COLOR) ?: 0xFF000000.toInt())
            setColorListener(ColorEnvelopeListener { envelope, _ ->
                onColorSelected?.invoke(envelope.color)
            })
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.select_color)
            .setView(colorPickerView)
            .setPositiveButton(android.R.string.ok, null)
            .create()
    }

    companion object {
        private const val ARG_INITIAL_COLOR = "initial_color"

        fun newInstance(initialColor: Int, onColorSelected: (Int) -> Unit): ColorPickerDialog {
            return ColorPickerDialog().apply {
                arguments = Bundle().apply {
                    putInt(ARG_INITIAL_COLOR, initialColor)
                }
                this.onColorSelected = onColorSelected
            }
        }
    }
} 