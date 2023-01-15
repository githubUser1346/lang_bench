package benchrunner

import process.ProcessBuilder
import java.nio.file.Paths

val bashPath = "/usr/bin/bash"
val cargoPath1 = "/usr/bin/cargo"
val cargoPath2 = "${System.getProperty("user.home")}/.cargo/bin/cargo"
val denoPath = "/usr/bin/deno"
val goPath = "/usr/bin/go"
val repoDir = System.getProperty("user.dir").substringBefore("/benchrunner/app")


fun main() {
    val benchRunner = BenchRunner()

    buildKotlin(benchRunner)
    buildRust(benchRunner)

    for (i in 0..3) {
        execKotlin(benchRunner)
        execRust(benchRunner)
        execGo(benchRunner)
    }
    println(benchRunner.stats.computeSummary())
}

private fun buildKotlin(benchRunner: BenchRunner) {
    val builder = ProcessBuilder("/usr/bin/bash gradlew build -x test")
    builder.directory(Paths.get("$repoDir/kotlin-jvm").toFile())
    benchRunner.build(builder)
}

private fun buildRust(benchRunner: BenchRunner) {
    val cargoPath = if (Paths.get(cargoPath2).toFile().exists()) cargoPath2 else cargoPath1
    val builder = ProcessBuilder("$cargoPath build --release")
    builder.directory(Paths.get("$repoDir/rust").toFile())
    benchRunner.build(builder)
}

private fun execKotlin(benchRunner: BenchRunner) {
    val builder = ProcessBuilder("$bashPath gradlew --quiet run -x build -x test")
    builder.directory(Paths.get("$repoDir/kotlin-jvm").toFile())
    benchRunner.exec(builder)
}

private fun execRust(benchRunner: BenchRunner) {
    val builder = ProcessBuilder("$repoDir/rust/target/release/rust1")
    benchRunner.exec(builder)
}

private fun execGo(benchRunner: BenchRunner) {
    val builder = ProcessBuilder("$goPath run main.go")
    builder.directory(Paths.get("$repoDir/golang").toFile())
    benchRunner.exec(builder)
}

private fun execJavascript(benchRunner: BenchRunner) {
    benchRunner.exec(ProcessBuilder("$denoPath run --allow-read js/main.js"))
}