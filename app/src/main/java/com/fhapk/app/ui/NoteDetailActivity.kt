package com.fhapk.app.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.fhapk.app.App
import com.fhapk.app.R
import com.fhapk.app.data.Note
import com.fhapk.app.databinding.ActivityNoteDetailBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

class NoteDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNoteDetailBinding
    private val repository by lazy { (application as App).repository }

    private lateinit var note: Note
    private var loading = false
    private var dirty = false
    private var saveJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val noteId = intent.getStringExtra(EXTRA_NOTE_ID)
        note = Note(id = noteId ?: UUID.randomUUID().toString())
        supportActionBar?.title = if (noteId == null) "Catatan baru" else ""

        if (noteId != null) {
            loading = true
            lifecycleScope.launch {
                val loaded = repository.getById(noteId)
                if (loaded != null) {
                    note = loaded
                    binding.etTitle.setText(loaded.title)
                    binding.etContent.setText(loaded.content)
                }
                loading = false
            }
        }

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!loading) {
                    dirty = true
                    scheduleSave()
                }
            }
        }
        binding.etTitle.addTextChangedListener(watcher)
        binding.etContent.addTextChangedListener(watcher)
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = lifecycleScope.launch {
            delay(500)
            saveNow()
        }
    }

    private fun saveNow() {
        saveJob?.cancel()
        saveJob = null
        if (!dirty) return
        dirty = false
        note = note.copy(
            title = binding.etTitle.text?.toString()?.trim().orEmpty(),
            content = binding.etContent.text?.toString().orEmpty(),
            updatedAt = System.currentTimeMillis()
        )
        val snapshot = note
        lifecycleScope.launch { repository.save(snapshot) }
    }

    override fun onPause() {
        saveNow()
        super.onPause()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_delete -> {
                confirmDelete()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_note_detail, menu)
        return true
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setMessage("Hapus catatan ini?")
            .setPositiveButton("Hapus") { _, _ ->
                lifecycleScope.launch {
                    repository.delete(note)
                    finish()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    companion object {
        private const val EXTRA_NOTE_ID = "extra_note_id"

        fun intent(context: Context, noteId: String?): Intent =
            Intent(context, NoteDetailActivity::class.java).apply {
                if (noteId != null) putExtra(EXTRA_NOTE_ID, noteId)
            }
    }
}