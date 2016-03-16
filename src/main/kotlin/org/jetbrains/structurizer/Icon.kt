package org.jetbrains.structurizer

import java.nio.file.Path
import java.util.*

class Icon(val content: ByteArray, path: Path) {
    protected val paths = ArrayList<Path>()

    init {
        paths.add(path)
    }

    fun getPaths(): List<String> {
        return paths.map { it.toString() }
    }

    fun getName() = paths[0].fileName.toString()

    fun addPath(path: Path) {
        if (paths.isNotEmpty() && paths[0].fileName != path.fileName) {
            throw IllegalArgumentException("All icon should have the same filename")
        }
        paths.add(path)
    }
}