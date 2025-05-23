// 리팩토링 계획:
// 1. EditorViewModel은 "상태 관리와 이력 처리"에 집중
// 2. DSLR 관련 로직은 DslrProcessor / DslrViewModel 쪽으로 분리
// 3. Upscale도 EnhancementViewModel 쪽으로 위임

package com.ruto.pthotoditor2.feature.editor.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.ruto.pthotoditor2.core.image.commonutil.HardwareBitmapConvert.ensureSoftwareConfig

import com.ruto.pthotoditor2.feature.editor.model.EditorMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.ArrayDeque
import javax.inject.Inject

@HiltViewModel
class EditorStateViewModel @Inject constructor() : ViewModel() {

    companion object {
        private const val MAX_HISTORY_SIZE = 10
    }

    // 이미지 상태
    private val _originalImage = MutableStateFlow<Bitmap?>(null)
    val originalImage = _originalImage.asStateFlow()

    private val _editedImage = MutableStateFlow<Bitmap?>(null)
    val editedImage = _editedImage.asStateFlow()

    // 편집 모드 (현재 어떤 모드를 쓰고있는가)
    private val _editorMode = MutableStateFlow(EditorMode.FILTER)
    val editorMode = _editorMode.asStateFlow()

    //작업진행중을 나타내는 인디케이터 설정 Processing 진행 되는지 체크
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    // Undo/Redo 스택
    private val undoStack = ArrayDeque<Bitmap>()
    private val redoStack = ArrayDeque<Bitmap>()

    fun setOriginal(bitmap: Bitmap) {
        _originalImage.value = bitmap
        _editedImage.value = bitmap
        undoStack.clear()
        redoStack.clear()
    }

    fun setEditedBitmap(bitmap: Bitmap) {
        _editedImage.value?.let { current ->
            val copy = current.ensureSoftwareConfig()
            if (undoStack.size >= MAX_HISTORY_SIZE) undoStack.removeFirst()
            undoStack.addLast(copy)
            redoStack.clear()
        }
        _editedImage.value = bitmap
    }

    fun setEditorMode(mode: EditorMode) {
        _editorMode.value = mode
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun undo() {
        if (!canUndo()) return
        _editedImage.value?.let { current ->
            val copy = current.ensureSoftwareConfig()
            if (redoStack.size >= MAX_HISTORY_SIZE) redoStack.removeFirst()
            redoStack.addLast(copy)
        }
        _editedImage.value = undoStack.removeLast()
    }

    fun redo() {
        if (!canRedo()) return
        _editedImage.value?.let { current ->
            val copy = current.ensureSoftwareConfig()
            if (undoStack.size >= MAX_HISTORY_SIZE) undoStack.removeFirst()
            undoStack.addLast(copy)
        }
        _editedImage.value = redoStack.removeLast()
    }

    fun reset() {
        _originalImage.value?.let {
            undoStack.clear()
            redoStack.clear()
            _editedImage.value = it.ensureSoftwareConfig()
        }
    }
}
