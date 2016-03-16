package org.jetbrains.structurizer.database

import org.jetbrains.exposed.sql.Table

object Icons : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val name = varchar("filename", length = 100)
    val image = blob("image")
}

object IconPaths : Table(name = "icon_paths") {
    val id = integer("id").autoIncrement().primaryKey()
    val icon = integer("icon") references Icons.id
    val path = text("path")
}