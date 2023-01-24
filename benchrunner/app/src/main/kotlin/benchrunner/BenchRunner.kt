@file:OptIn(ExperimentalSerializationApi::class)

package benchrunner

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import process.ProcessRunner

class BenchRunner() {
    val stats = BenchStats()

    fun exec(builder: ProcessBuilder) {
        val exitCode = ProcessRunner(builder).exec { line: String ->
            if (line.isNotBlank()) {
                if (line.trim().startsWith("{") && line.trim().endsWith("}")) {
                    val deserializer = BenchResult.serializer()
                    val result = jsonFormat.decodeFromString(deserializer, line)
                    println(result)
                    stats.add(result)
                } else {
                    println(line)
                }
            }
        }
        if (exitCode != 0) {
            throw RuntimeException("Process exited with $exitCode")
        }
    }

    fun build(builder: ProcessBuilder) {
        val exitCode: Int = ProcessRunner(builder).exec { }
        if (exitCode != 0) {
            throw RuntimeException("Build returned $exitCode")
        }
    }

    companion object {
        val jsonFormat = Json {
            ignoreUnknownKeys = true
        }
    }


}