package org.jetbrains.structurizer.database

import org.jetbrains.exposed.sql.*
import org.jetbrains.structurizer.Icon
import java.util.*
import javax.sql.rowset.serial.SerialBlob

fun initTables(dataBase: Database) {
    dataBase.transaction {
        create(Icons)
        create(IconPaths)
    }
}

fun getAllPaths(dataBase: Database): Map<String, Int> {
    val pathWithIds = HashMap<String, Int>()

    dataBase.transaction {
        IconPaths.slice(IconPaths.path, IconPaths.icon).selectAll().forEach {
            pathWithIds.put(it[IconPaths.path], it[IconPaths.icon])
        }
    }

    return pathWithIds
}

fun addIcons(icons: List<Icon>, dataBase: Database) {
    val allPaths = getAllPaths(dataBase)
    for (icon in icons) {
        val iconPathsInDatabase = allPaths.keys.intersect(icon.getPaths())

        if(iconPathsInDatabase.size != icon.getPaths().size) {
            var iconId: Int? = if (iconPathsInDatabase.isNotEmpty()) allPaths[iconPathsInDatabase.first()]!! else null

            dataBase.transaction {
                if (iconId == null) {
                    val blob = connection.createBlob()
                    blob.setBytes(1, icon.content)
                    iconId = Icons.insert {
                        it[name] = icon.getName()
                        it[image] = SerialBlob(icon.content);
                    } get Icons.id
                }

                for (path in icon.getPaths() - iconPathsInDatabase) {
                    IconPaths.insert {
                        it[IconPaths.icon] = iconId
                        it[IconPaths.path] = path
                    }
                }
            }
        }
    }
}