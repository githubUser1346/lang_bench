@file:OptIn(ExperimentalSerializationApi::class)

package benchrunner

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import process.ProcessRunner

class BenchRunner() {
    val stats = BenchStats()

    fun exec(builder: ProcessBuilder) {
        ProcessRunner(builder).exec { line: String ->
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
    }

    fun build(builder: ProcessBuilder) {
        val returnCode: Int = ProcessRunner(builder).exec { }
        if(returnCode!=0){
            throw RuntimeException("Build returned $returnCode")
        }
    }

    companion object {
        val jsonFormat = Json {
            ignoreUnknownKeys = true
        }
    }


}