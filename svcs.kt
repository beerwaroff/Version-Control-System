package svcs

import java.io.File
import java.security.MessageDigest
import java.util.*

var name = ""
var lastCommit = ""
var filesList = mutableListOf<String>()

fun main(args: Array<String>) {


    if (!File("vcs").isDirectory) File("vcs").mkdir()
    if (!File("vcs/commits").isDirectory) File("vcs/commits").mkdir()
    if (!File("vcs/config.txt").exists()) File("vcs/config.txt").createNewFile()
    else name = File("vcs/config.txt").readText()
    if (!File("vcs/index.txt").exists()) File("vcs/index.txt").createNewFile()
    else File("vcs/index.txt").forEachLine { filesList.add(it) }
    if (!File("vcs/log.txt").exists()) File("vcs/log.txt").createNewFile()
    if (File("vcs/log.txt").length() > 0) lastCommit = File("vcs/log.txt").readLines().first().split(" ")[1]

    val command = if (args.isNotEmpty()) args[0] else ""
    val parameters = if (args.size > 1) args[1] else ""

    when (command) {
        "", "--help" -> help()
        "config" -> config(parameters)
        "add" -> add(parameters)
        "log" -> log()
        "commit" -> commit(parameters)
        "checkout" -> checkout(parameters)
        else -> wrong(args[0])
    }
}

fun help() {
    println("""
These are SVCS commands:
config     Get and set a username.
add        Add a file to the index.
log        Show commit logs.
commit     Save changes.
checkout   Restore a file.
""")
}

fun config(parameters: String) {
    if (parameters.isEmpty() && File("vcs/config.txt").readText() == "") println("Please, tell me who you are.")
    else if ((parameters.isEmpty() && File("vcs/config.txt").readText() != "")) println("The username is ${File("vcs/config.txt").readText()}.")
    else {
        name = parameters
        println("The username is $name.")
        File("vcs/config.txt").writeText(name)
    }
}

fun add(parameters: String) {
    if (filesList.isEmpty() && parameters.isEmpty()) {
        println("Add a file to the index.")
    } else {
        if (parameters.isNotEmpty()) {
            if (File(parameters).exists()) {
                filesList.add(parameters)
                println("The File '$parameters' is tracked.")
                File("vcs/index.txt").appendText("$parameters\n")
            } else {
                println("Can't find '$parameters'.")
            }
        } else {
            println("Tracked files:")
            filesList.forEach {
                println(it)
            }
        }
    }
}

fun log() {
    if (File("vcs/log.txt").readText().isEmpty()) println("No commits yet.")
    else {
        File("vcs/log.txt").forEachLine {
            println(it)
        }
    }
}

fun commit(parameters: String) {
    if (parameters.isEmpty()) {
        println("Message was not passed.")
        return
    }

    val changeList = mutableListOf<String>()
    filesList.forEach {
        var fileDir = ""
        var fileCommit = ""
        if (File(it).exists()) fileDir = hash(File(it).readText())
        if (File("vcs/commits/$lastCommit/$it").exists()) fileCommit = hash(File("vcs/commits/$lastCommit/$it").readText())
        if (fileDir != fileCommit) changeList.add(it)
    }

    if (changeList.isEmpty()) println("Nothing to commit.")
    else {
        println("Changes are committed.")
        val uniqueID = UUID.randomUUID().toString()
        for (file in filesList) {
            File(file).copyTo(File("vcs/commits/$uniqueID/$file"))
        }
        val old = File("vcs/log.txt").readText()
        File("vcs/log.txt").writeText("commit $uniqueID\n")
        File("vcs/log.txt").appendText("Author: $name\n")
        File("vcs/log.txt").appendText("$parameters\n\n")
        File("vcs/log.txt").appendText(old)
    }
}

fun checkout(parameters: String) {
    if (parameters.isEmpty()) println("Commit id was not passed.")
    else {
        if (File("vcs/commits/$parameters").isDirectory) {
            val files = File("vcs/commits/$parameters").list()
            for (file in files) {
                File("vcs/commits/$parameters/$file").copyTo(File(file), overwrite = true)
            }
            println("Switched to commit $parameters.")
        } else println("Commit does not exist.")
    }
}

fun wrong(command: String) {
    println("'$command' is not a SVCS command.")
}

fun hash(input: String): String {
    return MessageDigest
        .getInstance("SHA-256")
        .digest(input.toByteArray())
        .fold("") { str, it -> str + "%02x".format(it) }
}