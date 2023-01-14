package benchrunner


private const val WARMUP_COUNT = 2

class BenchStats {

    private val resultsByBenchImpl = mutableMapOf<Pair<String, String>, MutableList<BenchResult>>()

    fun add(benchResult: BenchResult) {
        resultsByBenchImpl.computeIfAbsent(benchResult.bench to benchResult.impl) { mutableListOf() }.add(benchResult)
    }

    private fun computeAggregationPerBench(): Map<String, MutableList<BenchResultAggregation>> {
        val aggregationsPerBench = mutableMapOf<String, MutableList<BenchResultAggregation>>()
        resultsByBenchImpl.forEach { entry: Map.Entry<Pair<String, String>, MutableList<BenchResult>> ->
            val benchResults: MutableList<BenchResult> = entry.value
            // Skip the first WARMUP_COUNT results since they are only for warmup.
            val warmResults = if (benchResults.size > WARMUP_COUNT) benchResults.drop(WARMUP_COUNT) else benchResults
            val warmTimes = warmResults.map { it.ms.toDouble() }.toDoubleArray()
            val actual: String = warmResults.map { it.result }.toSet().enforceUnique()
            val mean = warmTimes.average().toLong()
            val meanDiff = warmTimes.map { kotlin.math.abs(it - mean) }.average()
            val bench = entry.key.first
            val impl = entry.key.second
            val aggregatedResult = BenchResultAggregation(impl, bench, mean, meanDiff.toLong(), actual, warmResults)
            aggregationsPerBench.computeIfAbsent(bench) { mutableListOf() }.add(aggregatedResult)
        }
        aggregationsPerBench.forEach { entry: Map.Entry<String, MutableList<BenchResultAggregation>> ->
            //Verify that the different implementation gave the same thing. Their 'actual' must be the same.
            val aggregations: MutableList<BenchResultAggregation> = entry.value
            val resultByActual = aggregations.groupBy({ it.actual }, { it.impl })
            if (resultByActual.size > 1) {
                throw RuntimeException("Benches didnt all give the same answer: bench=${entry.key}, actual=$resultByActual")
            }
            aggregations.sortBy { it.mean }
        }
        return aggregationsPerBench
    }

    fun computeSummary(): String {
        val sb = StringBuilder()
        sb.append("Benchmark Summary (Smaller percentage is better) (Ignored $WARMUP_COUNT warmups) (effort=${System.getenv("LANG_BENCH_EFFORT")}) \n")
        val aggregationsPerBench = computeAggregationPerBench()
        for (entry: Map.Entry<String, MutableList<BenchResultAggregation>> in aggregationsPerBench) {
            val benchName = entry.key
            val denominator: Float = entry.value.maxOf { it.mean }.toFloat()
            sb.append("  %-32s ".format(benchName)).append("scale:%6.0fms".format(denominator)).append("\n")
            for (result in entry.value) {
                val normalizedMean = result.mean / denominator * 100
                val normalizedMeanDiff = result.meanDiff / denominator * 100
                sb.append(
                    "    %-32s mean:%3.0f%%, meanDiff:%3.0f%%".format(
                        result.impl,
                        normalizedMean,
                        normalizedMeanDiff
                    )
                ).append("\n")
            }
        }
        return sb.toString()
    }
}

fun <T> Set<T>.enforceUnique(): T {
    require(this.size == 1) {
        "This set must contain exactly one element. $this"
    }
    return first()
}