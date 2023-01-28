package benchrunner

import process.ProcessBuilder
import java.nio.file.Paths

val bashPath = "/usr/bin/bash"
val cargoPathPrimary = "${System.getProperty("user.home")}/.cargo/bin/cargo"
val cargoPathSecondary = "/usr/bin/cargo"
val goPath = "/usr/bin/go"
val gccPath = "/usr/bin/gcc"
val repoDir = System.getProperty("user.dir").substringBefore("/benchrunner/app")

fun main() {
    val benchRunner = BenchRunner()

    buildKotlin(benchRunner)
    buildRust(benchRunner)
    build_c(benchRunner)

    for (i in 0..3) {
        execKotlin(benchRunner)
        execRust(benchRunner)
        execGo(benchRunner)
        exec_c(benchRunner)
    }
    println(benchRunner.stats.computeSummary())
}

private fun buildKotlin(benchRunner: BenchRunner) {
    val builder = ProcessBuilder("/usr/bin/bash gradlew build -x test")
    builder.directory(Paths.get("$repoDir/kotlin-jvm").toFile())
    benchRunner.build(builder)
}

private fun buildRust(benchRunner: BenchRunner) {
    val cargoPath = if (Paths.get(cargoPathPrimary).toFile().exists()) cargoPathPrimary else cargoPathSecondary
    val builder = ProcessBuilder("$cargoPath build --release")
    builder.directory(Paths.get("$repoDir/rust").toFile())
    benchRunner.build(builder)
}

private fun build_c(benchRunner: BenchRunner) {
    val builder = ProcessBuilder("$gccPath -march=native -O3 -funroll-all-loops main.c")
    builder.directory(Paths.get("$repoDir/c").toFile())
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

private fun exec_c(benchRunner: BenchRunner) {
    val builder = ProcessBuilder("$repoDir/c/a.out")
    benchRunner.exec(builder)
}

