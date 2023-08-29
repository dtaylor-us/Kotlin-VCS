package svcs

import java.io.File
import java.security.MessageDigest

private const val ROOT_PATH = "vcs"

data class Commit(val id: String, val author: String, val message: String)


fun main(args: Array<String>) {
    initRootDirectory()

    when (args.getOrNull(0)) {
        "--help", null -> menu()
        "config" -> config(args.getOrNull(1))
        "add" -> add(args.getOrNull(1))
        "log" -> log()
        "commit" -> {
            val message = args.getOrNull(1)
            if (message != null) {
                commit(message)
            } else {
                println("Message was not passed.")
            }
        }

        "checkout" -> {
            val commitId = args.getOrNull(1)
            if (commitId != null) {
                checkout(commitId)
            } else {
                println("Commit id was not passed.")
            }
        }

        else -> println("'${args[0]}' is not a SVCS command.")
    }
}

fun checkout(commitId: String) {
    val commitDir = File("$ROOT_PATH/commits/$commitId")
    if (!commitDir.exists()) {
        print("Commit does not exist.")
        return
    }

    commitDir.listFiles()?.forEach { file ->
        val destFile = File(file.name)
        if (destFile.exists()) {
            file.copyTo(destFile, overwrite = true)
        }
    }

    println("Switched to commit $commitId.")

}

fun commit(message: String) {
    val trackedFiles = getTrackedFile().readLines().drop(1)
    if (trackedFiles.isEmpty()) {
        println("Nothing to commit.")
        return
    }

    val hash = generateHash(trackedFiles)
    val commitDir = File("$ROOT_PATH/commits/$hash")

    if (commitDir.exists()) {
        println("Nothing to commit.")
        return
    }

    commitDir.mkdirs()

    trackedFiles.forEach { fileName ->
        val source = File(fileName.trim())
        if (source.exists()) {
            source.copyTo(File(commitDir, source.name))
        }
    }

    val logFile = getLogFile()
    val user = getConfigFile().readLines().firstOrNull() ?: "Unknown"

    logFile.appendText(
        "$message\nAuthor: $user\ncommit $hash\n\n"
    )

    println("Changes are committed.")
}

private fun generateHash(trackedFiles: List<String>): String {
    val concatFiles = trackedFiles.joinToString("") { fileName ->
        val file = File(fileName.trim())
        if (file.exists()) file.readText() else ""
    }
    return MessageDigest.getInstance("SHA-1")
        .digest((concatFiles).toByteArray())
        .joinToString("") { "%02x".format(it) }
}


fun log() {
    val logFile = getLogFile()
    if (!logFile.exists() || logFile.readText().isEmpty()) {
        println("No commits yet.")
    } else {
        logFile.readLines().reversed().forEach { println(it) }
    }
}

private fun getLogFile() = File("$ROOT_PATH/log.txt")

private fun menu() = print(
    """These are SVCS commands:
        |config     Get and set a username.
        |add        Add a file to the index.
        |log        Show commit logs.
        |commit     Save changes.
        |checkout   Restore a file.""".trimMargin()
)

private fun config(arg: String?) {
    val configFile = getConfigFile()
    when {
        arg == null && configFile.exists() && configFile.readLines().isNotEmpty() ->
            println("The username is ${configFile.readLines()[0]}.")

        arg == null -> println("Please, tell me who you are.")
        else -> {
            configFile.writeText(arg)
            println("The username is $arg.")
        }
    }
}

private fun add(arg: String?) {
    when (arg) {
        null -> {
            val fileNames = getTrackedFile().readLines()
            if (fileNames.size == 1) {
                println("Add a file to the index.")
            } else {
                println(getTrackedFile().readText())
            }
        }

        else -> {
            val file = File(arg)
            if (file.exists()) {
                getTrackedFile().appendText("\n$arg")
                println("The file '$arg' is tracked.")
            } else {
                println("Can't find '$arg'.")
            }
        }
    }
}

private fun initRootDirectory() {
    val root = File(ROOT_PATH)
    if (!root.isDirectory && root.mkdirs()) {
        getTrackedFile().apply {
            createNewFile()
            writeText("Tracked files:")
        }
        getConfigFile().createNewFile()
    } else if (!root.isDirectory) {
        println("Failed to create directory: $ROOT_PATH")
    }
}

private fun getTrackedFile() = File("$ROOT_PATH/index.txt")
private fun getConfigFile() = File("$ROOT_PATH/config.txt")
