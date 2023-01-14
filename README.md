This project contains simple benchmarks for different languages.

# Env

This file assumes that java, cargo and go are installed.
```
benchrunner/app/src/main/kotlin/benchrunner/App.kt
```

# Usage

Run the benchmarks with this.
```bash
cd language_benchmarks/benchrunner
./gradlew run
```
Or from an IDE by running this file
```
benchrunner/App.kt
```

# Methodology

There are many ways we can build a benchmark.
Here are a few things that describe the benchmarks we have here.
  
- Unlike some other benchmarks, we dont measure the process execution time. Instead, what is measured is a loop running a small task many times. 
- Each task is run many times before we start measuring it. We call that the warmup.
- We try to target only the cpu efficiency. Each task uses little memory, and no IO.
- These tasks cannot be optimized away by compilers since 1) they operate on nondeterministic data 2) their result is eventually printed in stdout 3) each task iteration work on different data.     
- All tasks compute a nonsensical number that means nothing.
- These nonsensical numbers must be the same for the different implementation of a task, or the benchmark fails.
- The GC is run before starting the measurements in kotlin and go.
- 