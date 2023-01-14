package process

import java.io.BufferedReader
import java.io.InputStream
import java.nio.file.Paths
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CompletableFuture


class ProcessEventStreamer(val builder: ProcessBuilder) {


    interface ProcessEvent
    interface ProcessEnd : ProcessEvent
    data class OutputLine(val line: String) : ProcessEvent
    data class ErrorLine(val line: String) : ProcessEvent
    data class ExitCode(val value: Int) : ProcessEnd
    data class ExecutionError(val message: String) : ProcessEnd

    val queue = ArrayBlockingQueue<ProcessEvent>(128)

    fun runAsync() {
        if (builder.directory() != null) {
            val workDir = builder.directory().absoluteFile
            if (!workDir.exists()) {
                throw RuntimeException("Invalid work directory: $workDir")
            }
        }
        val exec = Paths.get(builder.command()[0]).toAbsolutePath().toFile()
        if (!exec.exists()) {
            throw RuntimeException("Invalid executable: $exec")
        }
        val process = try {
            builder.start()
        } catch (e: Exception) {
            queue.put(ExecutionError(e.message ?: "Unknown Error."))
            return;
        }
        val stderrCf = consume(process.errorStream, true)
        val stdoutCf = consume(process.inputStream, false)
        CompletableFuture.runAsync {
            CompletableFuture.allOf(stderrCf, stdoutCf).join()
            val exitCode = process.waitFor()
            queue.put(ExitCode(exitCode))
        }
    }

    private fun consume(inputStream: InputStream, error: Boolean): CompletableFuture<Void> {
        val runAsync: CompletableFuture<Void> = CompletableFuture.runAsync {
            val reader = BufferedReader(inputStream.reader())
            reader.use {
                var line = it.readLine()
                while (line != null) {
                    if (error) {
                        queue.put(ErrorLine(line))
                    } else {
                        queue.put(OutputLine(line))
                    }
                    line = it.readLine()
                }
            }
        }
        return runAsync
    }


}