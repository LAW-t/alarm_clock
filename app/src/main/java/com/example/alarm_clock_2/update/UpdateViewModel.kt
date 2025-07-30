package com.example.alarm_clock_2.update

import android.app.DownloadManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val updateRepository: UpdateRepository
) : ViewModel() {

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog: StateFlow<Boolean> = _showUpdateDialog.asStateFlow()

    private val _showDownloadDialog = MutableStateFlow(false)
    val showDownloadDialog: StateFlow<Boolean> = _showDownloadDialog.asStateFlow()

    private val _showInstallDialog = MutableStateFlow(false)
    val showInstallDialog: StateFlow<Boolean> = _showInstallDialog.asStateFlow()

    private var currentDownloadId: Long? = null

    /**
     * Check for updates
     */
    fun checkForUpdates(currentVersion: String, showProgress: Boolean = false) {
        viewModelScope.launch {
            if (showProgress) {
                _updateState.value = UpdateState.Checking
            }

            try {
                val result = updateRepository.checkForUpdates(currentVersion)
                
                result.fold(
                    onSuccess = { updateInfo ->
                        if (updateInfo != null) {
                            _updateState.value = UpdateState.UpdateAvailable(updateInfo)
                            _showUpdateDialog.value = true
                        } else {
                            _updateState.value = UpdateState.NoUpdateAvailable
                        }
                    },
                    onFailure = { exception ->
                        _updateState.value = UpdateState.Error(
                            message = "检查更新失败: ${exception.message}",
                            canRetry = true
                        )
                    }
                )
            } catch (e: Exception) {
                _updateState.value = UpdateState.Error(
                    message = "检查更新时发生错误: ${e.message}",
                    canRetry = true
                )
            }
        }
    }

    /**
     * Start downloading update
     */
    fun startDownload(updateInfo: UpdateInfo) {
        viewModelScope.launch {
            try {
                val downloadId = updateRepository.downloadUpdate(updateInfo)
                currentDownloadId = downloadId
                
                _updateState.value = UpdateState.Downloading(0, downloadId)
                _showUpdateDialog.value = false
                _showDownloadDialog.value = true
                
                // Monitor download progress
                monitorDownloadProgress(downloadId)
                
            } catch (e: Exception) {
                _updateState.value = UpdateState.Error(
                    message = "开始下载失败: ${e.message}",
                    canRetry = true
                )
            }
        }
    }

    /**
     * Monitor download progress
     */
    private fun monitorDownloadProgress(downloadId: Long) {
        viewModelScope.launch {
            while (true) {
                val progress = updateRepository.getDownloadProgress(downloadId)
                
                if (progress == null) {
                    _updateState.value = UpdateState.Error(
                        message = "下载进度获取失败",
                        canRetry = true
                    )
                    break
                }

                when (progress.status) {
                    DownloadManager.STATUS_RUNNING -> {
                        _updateState.value = UpdateState.Downloading(progress.progress, downloadId)
                    }
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        val filePath = updateRepository.getDownloadedFilePath(downloadId)
                        if (filePath != null) {
                            _updateState.value = UpdateState.DownloadComplete(filePath)
                            _showDownloadDialog.value = false
                            _showInstallDialog.value = true
                        } else {
                            _updateState.value = UpdateState.Error(
                                message = "下载完成但无法找到文件",
                                canRetry = true
                            )
                        }
                        break
                    }
                    DownloadManager.STATUS_FAILED -> {
                        _updateState.value = UpdateState.Error(
                            message = "下载失败",
                            canRetry = true
                        )
                        break
                    }
                    DownloadManager.STATUS_PAUSED -> {
                        // Continue monitoring
                    }
                }

                delay(1000) // Check every second
            }
        }
    }

    /**
     * Cancel current download
     */
    fun cancelDownload() {
        currentDownloadId?.let { downloadId ->
            updateRepository.cancelDownload(downloadId)
            _updateState.value = UpdateState.Idle
            _showDownloadDialog.value = false
            currentDownloadId = null
        }
    }

    /**
     * Postpone update
     */
    fun postponeUpdate(version: String) {
        viewModelScope.launch {
            updateRepository.postponeUpdate(version)
            _updateState.value = UpdateState.Idle
            _showUpdateDialog.value = false
        }
    }

    /**
     * Retry failed operation
     */
    fun retry(currentVersion: String) {
        when (val state = _updateState.value) {
            is UpdateState.Error -> {
                if (state.canRetry) {
                    checkForUpdates(currentVersion, true)
                }
            }
            else -> {
                checkForUpdates(currentVersion, true)
            }
        }
    }

    /**
     * Dismiss update dialog
     */
    fun dismissUpdateDialog() {
        _showUpdateDialog.value = false
    }

    /**
     * Dismiss download dialog
     */
    fun dismissDownloadDialog() {
        _showDownloadDialog.value = false
    }

    /**
     * Dismiss install dialog
     */
    fun dismissInstallDialog() {
        _showInstallDialog.value = false
    }

    /**
     * Reset update state
     */
    fun resetState() {
        _updateState.value = UpdateState.Idle
        _showUpdateDialog.value = false
        _showDownloadDialog.value = false
        _showInstallDialog.value = false
        currentDownloadId = null
    }

    /**
     * Clear postponed update (when user manually checks)
     */
    fun clearPostponedUpdate() {
        viewModelScope.launch {
            updateRepository.clearPostponedUpdate()
        }
    }
}
