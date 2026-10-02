@file:OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)
package org.example.example4
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.time.Clock
import kotlin.time.Duration

typealias RequestType = () -> Unit

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
class SimpleActor {
    private val requests = mutableListOf<RequestType>()

    init {
        Thread {
            while (true) {
                val request = synchronized(requests) {
                    val request = requests.removeFirstOrNull()
                    if (request == null) {
                        (requests as Object).wait()
                        continue
                    }
                    request
                }
                request()
            }
        }.apply {
            isDaemon = true
            start()
        }
    }

    /**
     * Sends a request that will execute on the thread of this actor.
     *
     * Requests to any given actor execute one-by-one,
     * never in parallel.
     */
    fun sendRequest(
        request: RequestType,
    ) {
        synchronized(requests) {
            requests.add(request)
            (requests as Object).notify()
        }
    }
}

fun scheduleReminder(
    printlnActor: SimpleActor,
    remindIn: Duration,
    reminderText: List<String>
) {
    Thread {
        Thread.sleep(remindIn.inWholeMilliseconds)
        val stringToPrint = buildString {
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
        }
        printlnActor.sendRequest {
            println(stringToPrint)
        }
    }.apply {
        isDaemon = false
        start()
    }
}

// NEW APIS ///////////////////////////////////////////////////////////////////

enum class LoopIterationResult {
    BREAK,
    CONTINUE,
    WILL_BE_RESUMED_ASYNCHRONOUSLY
}

/**
 * Runs an infinite loop asynchronously.
 *
 * [schedule] is the command that will be invoked to schedule
 * the next loop iteration.
 *
 * [iteration] is the loop body.
 * It accepts a value of type `() -> Unit` invoking which will cause
 * the next iteration to be scheduled.
 * Use this in conjunction with
 * [LoopIterationResult.WILL_BE_RESUMED_ASYNCHRONOUSLY].
 *
 * [actionOnLoopExit] will be run once the loop exits.
 *
 * This function returns immediately after scheduling the first iteration.
 */
fun runInfiniteLoop(
    schedule: (RequestType) -> Unit,
    iteration: (scheduleNextIteration: () -> Unit) -> LoopIterationResult,
    actionOnLoopExit: () -> Unit,
) {
    fun helper() {
        when (iteration { schedule(::helper) }) {
            LoopIterationResult.BREAK -> {
                /* We exited the loop. No need to reschedule anything. */
                actionOnLoopExit()
            }
            LoopIterationResult.CONTINUE -> {
                /* Schedule the next loop iteration */
                schedule(::helper)
            }
            LoopIterationResult.WILL_BE_RESUMED_ASYNCHRONOUSLY -> {
                /* No need to do anything, someone else will resume the loop. */
            }
        }
    }
    schedule(::helper)
}

// NEW API USAGE //////////////////////////////////////////////////////////////

val shouldTerminate = AtomicBoolean(false)

fun main() {
    val printlnActor = SimpleActor()
    val userInputActor = SimpleActor()

    runInfiniteLoop(
        schedule = userInputActor::sendRequest,
        iteration = iteration@{ scheduleNextIteration ->
            printlnActor.sendRequest {
                println("Please enter your input:")
            }
            val input = readlnOrNull()
                ?: return@iteration LoopIterationResult.BREAK
            val command = input
                .trim().split("\\s".toRegex())
                .toMutableList()
            val operation = command.removeFirstOrNull()
                ?: return@iteration LoopIterationResult.CONTINUE
            when (val operationInLowerCase = operation.lowercase()) {
                "remind" -> {
                    val remindIn = command.removeFirstOrNull()
                    val remindInParsed = remindIn?.let(Duration::parseOrNull)
                    if (remindInParsed == null) {
                        printlnActor.sendRequest {
                            println(buildString {
                                append("The 'remind' command requires a 'Duration' argument")
                                if (remindIn != null) {
                                    append(", got '$remindIn'")
                                }
                            })
                            println()
                            printRemindSyntax()
                        }
                    } else {
                        scheduleReminder(printlnActor, remindInParsed, command)
                        printlnActor.sendRequest {
                            println("Scheduled a reminder in $remindInParsed")
                            println("    (fires at ${Clock.System.now() + remindInParsed})")
                        }
                    }
                }
                "help" -> printlnActor.sendRequest {
                    printHelp()
                }
                "fun_animation" -> {
                    printlnActor.sendRequest {
                        for (frame in funAnimationFrames) {
                            println(frame)
                            Thread.sleep(200) // 200 milliseconds
                        }
                        userInputActor.sendRequest {
                            scheduleNextIteration()
                        }
                    }
                    return@iteration LoopIterationResult.WILL_BE_RESUMED_ASYNCHRONOUSLY
                }
                "quit" -> {
                    return@iteration LoopIterationResult.BREAK
                }
                else -> printlnActor.sendRequest {
                    println("Unknown command '$operationInLowerCase'")
                    println()
                    printHelp()
                }
            }
            LoopIterationResult.CONTINUE
        },
        actionOnLoopExit = {
            printlnActor.sendRequest {
                println("All done! The program will exit once all pending reminders fire.")
                // Allow the main thread to exit
                shouldTerminate.store(true)
            }
        }
    )
    // Keep the program alive: if we simply exit the main thread,
    // everything will terminate.
    while (!shouldTerminate.load()) {
        Thread.sleep(100)
    }
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
