package com.fhapk.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.fhapk.app.App
import com.fhapk.app.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val repository by lazy { (application as App).repository }
    private lateinit var adapter: NoteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        adapter = NoteAdapter { note ->
            startActivity(NoteDetailActivity.intent(this, note.id))
        }
        binding.rvNotes.layoutManager = LinearLayoutManager(this)
        binding.rvNotes.adapter = adapter

        binding.fabAdd.setOnClickListener {
            startActivity(NoteDetailActivity.intent(this, null))
        }

        lifecycleScope.launch {
            repository.observeAll().collect { notes ->
                adapter.submitList(notes)
            }
        }
    }
}