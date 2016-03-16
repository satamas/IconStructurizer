package org.jetbrains.structurizer

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Options
import org.jetbrains.exposed.sql.Database
import org.jetbrains.structurizer.database.addIcons
import org.jetbrains.structurizer.database.initTables
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

val fileModifiers = HashSet<String>()

fun main(args: Array<String>) {
    val options = Options()
    options.addOption("i", "icons", true, "Icons directory")
    val parser = DefaultParser()
    val cmd = parser.parse(options, args)
    val iconsDirPath = Paths.get(cmd.getOptionValue('i') ?: throw Exception("Icons dir should be specified"))

    val dataSource = MysqlDataSource()
    dataSource.serverName = "localhost"
    dataSource.portNumber = 3306
    dataSource.databaseName = "icons"
    dataSource.user = "root"
    dataSource.setPassword("admin")
    val database = Database.connect(dataSource)

    initTables(database)

    val icons = getAllIcons(iconsDirPath)
    fileModifiers.forEach { System.out.println(it) }
    //    addIcons(icons, database)
}

fun getAllIcons(iconsDirPath: Path): List<Icon> {
    val allIcons = ArrayList<Icon>()

    val iconsDir = iconsDirPath.toFile()
    if (!iconsDir.exists() || !iconsDir.isDirectory) {
        System.out.print("Icons directory should be valid directory path")
    }

    val sortedByNameIcons = HashMap<String, MutableList<Icon>>()

    for (file in iconsDir.walk()) {
        if (!file.isDirectory) continue
        addIconsFromFolder(iconsDirPath, file, sortedByNameIcons)
    }

    return sortedByNameIcons.values.flatten()
}

fun String.hasCommonPrefx(other: String): Boolean {
    return (this.decapitalize().split(Regex("[A-Z_@\\d]"))[0] == other.decapitalize().split(Regex("[A-Z_@\\d]"))[0])
}

fun addIconsFromFolder(iconsDirPath: Path, folder: File, sortedByNameIcons: MutableMap<String, MutableList<Icon>>) {
    val directoryIcons = folder.listFiles().filter {
        !it.isDirectory && it.name.endsWith(".png")
    }
    for (i in 0..(directoryIcons.size - 1)) {
        val file = directoryIcons[i]
        val iconName = file.name
        val iconPath = iconsDirPath.relativize(file.toPath())
        val iconContent = file.readBytes()

        // We don't need empty test icons with size 0 and large images, that are not icons
        // 65536 is size of blob in sql
        if (iconContent.size == 0 || iconContent.size > 65536) continue;

        val iconsWithSameName = sortedByNameIcons[iconName]
        if (iconsWithSameName != null) {
            var similarIcon: Icon? = iconsWithSameName.firstOrNull {
                Arrays.equals(it.content, iconContent);
            }

            if (similarIcon == null) {
                iconsWithSameName.add(Icon(iconContent, iconPath))
            } else {
                similarIcon.addPath(iconPath)
            }
        } else {
            sortedByNameIcons[iconName] = mutableListOf(Icon(iconContent, iconPath))
        }

        for (j in (i + 1)..(directoryIcons.size - 1)) {
            val anotherFile = directoryIcons[j]
            if (file.name.hasCommonPrefx(anotherFile.name) && file.name != anotherFile.name) {
                System.out.println()
            }
        }
    }
}