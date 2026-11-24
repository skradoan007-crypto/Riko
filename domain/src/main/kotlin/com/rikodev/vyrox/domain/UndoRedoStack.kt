package com.rikodev.vyrox.domain

class UndoRedoStack<T>(private val maxHistory: Int = 50) {
    private val undoDeque = ArrayDeque<T>()
    private val redoDeque = ArrayDeque<T>()
    private var current: T? = null

    val canUndo: Boolean get() = undoDeque.isNotEmpty()
    val canRedo: Boolean get() = redoDeque.isNotEmpty()

    fun initialize(initial: T) {
        undoDeque.clear()
        redoDeque.clear()
        current = initial
    }

    fun push(next: T) {
        val prev = current
        if (prev != null && prev == next) return
        if (prev != null) {
            undoDeque.addLast(prev)
            while (undoDeque.size > maxHistory) undoDeque.removeFirst()
        }
        redoDeque.clear()
        current = next
    }

    fun undo(): T? {
        if (undoDeque.isEmpty()) return null
        val cur = current ?: return null
        redoDeque.addLast(cur)
        val restored = undoDeque.removeLast()
        current = restored
        return restored
    }

    fun redo(): T? {
        if (redoDeque.isEmpty()) return null
        val cur = current ?: return null
        undoDeque.addLast(cur)
        val restored = redoDeque.removeLast()
        current = restored
        return restored
    }
}
