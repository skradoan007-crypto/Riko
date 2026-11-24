package com.rikodev.vyrox.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UndoRedoStackTest {
    @Test
    fun `undo and redo traverse history cleanly and clear redo on new branch`() {
        val stack = UndoRedoStack<String>(maxHistory = 10)
        stack.initialize("v0")
        stack.push("v1")
        stack.push("v2")

        assertThat(stack.canUndo).isTrue()
        assertThat(stack.undo()).isEqualTo("v1")
        assertThat(stack.canRedo).isTrue()
        assertThat(stack.redo()).isEqualTo("v2")

        assertThat(stack.undo()).isEqualTo("v1")
        stack.push("v3")
        assertThat(stack.canRedo).isFalse()
        assertThat(stack.undo()).isEqualTo("v1")
    }
}
