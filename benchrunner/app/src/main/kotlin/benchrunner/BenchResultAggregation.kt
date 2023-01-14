package benchrunner

data class BenchResultAggregation(
    val impl: String,
    val bench: String,
    val mean: Long,
    val meanDiff: Long,
    val actual: String,
    val results: List<BenchResult>
)