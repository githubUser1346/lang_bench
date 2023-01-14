package process

class ProcessRunner(val builder: ProcessBuilder) {

    fun exec(lineConsumer: (String) -> Unit = { }): Int {
        println("################################################")
        println("# command: " + builder.command().joinToString(separator = " "))
        println("# workdir: " + builder.directory())
//        println("# env    : " + builder.environment())
//        println("########################")
        val streamer = ProcessEventStreamer(builder)
        streamer.runAsync()

        while (true) {
            when (val event = streamer.queue.take()) {
                is ProcessEventStreamer.ExecutionError -> {
//                    println("########################")
                    println("# executionError: ${red(event.message)}")
//                    println("########################")
                    break
                }
                is ProcessEventStreamer.OutputLine -> {
                    lineConsumer(event.line)
                }
                is ProcessEventStreamer.ErrorLine -> {
                    println(event.line)
                }
                is ProcessEventStreamer.ExitCode -> {
//                    println("########################")
                    val exitCode = event.value
                    println("# exitCode: $exitCode")
//                    println("########################")
                    return exitCode
                }
                else -> throw RuntimeException("Unknown event: $event")
            }
        }
        // Never reached
        return -1
    }

    private fun red(string: String) = "${ANSI_RED}$string${ANSI_RESET}"

    companion object {
        const val ANSI_RED = "\u001B[31m"
        const val ANSI_RESET = "\u001B[0m"
    }
}

fun ProcessRunner(command: String) = ProcessRunner(ProcessBuilder(command))
fun ProcessRunner(command: List<String>) = ProcessRunner(ProcessBuilder(command))
fun ProcessBuilder(command: String) = ProcessBuilder(command.split(blankRegex))
private val blankRegex = Regex("\\s+")