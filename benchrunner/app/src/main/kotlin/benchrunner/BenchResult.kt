package benchrunner

import kotlinx.serialization.Serializable

@Serializable
data class BenchResult(
    val impl: String,
    val bench: String,
    val result: String,
    val ms: Long
)