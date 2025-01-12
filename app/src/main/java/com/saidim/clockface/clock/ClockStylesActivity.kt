package com.saidim.clockface.clock

import android.os.Bundle
import android.graphics.Color
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.saidim.clockface.R
import kotlinx.coroutines.launch
import com.google.android.material.color.MaterialColors
import androidx.core.view.WindowCompat
import android.content.res.Configuration
import com.saidim.clockface.base.BaseActivity

class ClockStylesActivity : BaseActivity() {
    private val viewModel: ClockStylesViewModel by viewModels()
    private lateinit var clockStylesAdapter: ClockStylesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_clock_styles)

        setupToolbar()
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupToolbar() {
        findViewById<androidx.appcompat.widget.Toolbar>(R.id.topAppBar).setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        clockStylesAdapter = ClockStylesAdapter { style ->
            viewModel.setClockStyle(style)
        }
        findViewById<RecyclerView>(R.id.clockStylesRecyclerView).apply {
            layoutManager = LinearLayoutManager(this@ClockStylesActivity)
            adapter = clockStylesAdapter
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.clockStyles.collect { styles ->
                clockStylesAdapter.submitList(styles)
            }
        }
    }
} 