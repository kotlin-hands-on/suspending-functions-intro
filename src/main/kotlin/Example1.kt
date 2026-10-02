package org.example.example1

import kotlin.time.Clock
import kotlin.time.Duration

fun scheduleReminder(remindIn: Duration, reminderText: List<String>) {
    Thread {
        Thread.sleep(remindIn.inWholeMilliseconds)
        println(buildString {
            append("*** REMINDER (SCHEDULED $remindIn AGO)")
            if (reminderText.isNotEmpty()) {
                append(": ")
                for (word in reminderText) {
                    append(word)
                    append(' ')
                }
            } else {
                append(' ')
            }
            append("***")
        })
    }.apply {
        isDaemon = false
        start()
    }
}

fun main() {
    while (true) {
        println("Please enter your input:")
        val input = readlnOrNull() ?: return
        val command = input
            .trim().split("\\s".toRegex())
            .toMutableList()
        val operation = command.removeFirstOrNull() ?: continue
        when (val operationInLowerCase = operation.lowercase()) {
            "remind" -> {
                val remindIn = command.removeFirstOrNull()
                val remindInParsed = remindIn?.let(Duration::parseOrNull)
                if (remindInParsed == null) {
                    println(buildString {
                        append("The 'remind' command requires a 'Duration' argument")
                        if (remindIn != null) {
                            append(", got '$remindIn'")
                        }
                    })
                    println()
                    printRemindSyntax()
                } else {
                    scheduleReminder(remindInParsed, command)
                    println("Scheduled a reminder in $remindInParsed")
                    println("    (fires at ${Clock.System.now() + remindInParsed})")
                }
            }
            "help" -> printHelp()
            "fun_animation" -> {
                for (frame in funAnimationFrames) {
                    println(frame)
                    Thread.sleep(200) // 200 milliseconds
                }
            }
            "quit" -> break
            else -> {
                println("Unknown command '$operationInLowerCase'")
                println()
                printHelp()
            }
        }
    }
    println("All done! The program will exit once all pending reminders fire.")
}

private fun printHelp() {
    println("Usage:")
    printRemindSyntax()
    printFunAnimationSyntax()
    printHelpSyntax()
    printQuitSyntax()
    println()
}

private fun printRemindSyntax() {
    println("remind duration [message] - remind about something")
    println("\texample: remind 10m Turn off stove")
}

private fun printFunAnimationSyntax() {
    println("fun_animation - show a fun animation to pass the time")
}

private fun printHelpSyntax() {
    println("help - show a list of available commands")
}

private fun printQuitSyntax() {
    println("quit - exit this program")
}

private val funAnimationFrames = (
    "#       _---_#     / o o o \\#    <=========>####       o#    " +
    "  /|\\#      / \\#~@##       _---_#     / o o o \\#    <=======" +
    "==>###       o#      /|\\#      / \\#~@###       _---_#     / " +
    "o o o \\#    <=========>##       o#      /|\\#      / \\#~@## " +
    "      _---_#     / o o o \\#    <=========>#     /       \\# " +
    "   /         \\#   /    o      \\#       /|\\#       / \\#~@## " +
    "      _---_#     / o o o \\#    <=========>#     /       \\# " +
    "   /   o     \\#   /   /|\\     \\#       / \\##~@##       _---" +
    "_#     / o o o \\#    <=========>#     /  o    \\#    /  /|\\" +
    "    \\#   /   / \\     \\###~@##       _---_#     / o o o \\#  " +
    "  <=========>#     /       \\#    /         \\#   /          " +
    " \\###~@##       _---_#     / - - - \\#    <=========>######~" +
    "@#       _---_#     / - - - \\#    <=========>#       * * *#" +
    "#####~@       * * *#         *#########~"
).replace("~", "==============================")
    .replace("#", "\n")
    .split("@")
