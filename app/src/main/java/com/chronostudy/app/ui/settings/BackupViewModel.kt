package com.chronostudy.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronostudy.app.data.backup.BackupRepository
import com.chronostudy.app.data.backup.ImportResult
import com.chronostudy.app.data.backup.ImportStrategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BackupViewModel(
    private val backup: BackupRepository,
) : ViewModel() {

    private val _exporting = MutableStateFlow(false)
    val exporting: StateFlow<Boolean> = _exporting.asStateFlow()

    private val _result = MutableStateFlow<ImportResult?>(null)
    val result: StateFlow<ImportResult?> = _result.asStateFlow()

    fun buildExport(onReady: (String) -> Unit) {
        _exporting.value = true
        viewModelScope.launch {
            val json = runCatching { backup.exportJson() }.getOrNull()
            _exporting.value = false
            if (json != null) onReady(json)
        }
    }

    fun import(raw: String, strategy: ImportStrategy) {
        viewModelScope.launch { _result.value = backup.importJson(raw, strategy) }
    }

    fun clearResult() {
        _result.value = null
    }
}
