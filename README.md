This project contains simple benchmarks for different languages.

# Usage

From benchrunner/Dockerfile

```
sudo docker build  -t bench1 /home/dev/github/lang_bench/benchrunner ; sudo docker run -e LANG_BENCH_EFFORT=4 -it --rm bench1
```

From an IDE by running

```
benchrunner/App.kt
```

From gradlew

```bash
cd language_benchmarks/benchrunner
LANG_BENCH_EFFORT=4 ./gradlew run
```

# Env

The LANG_BENCH_EFFORT environment variable can be used to configure how long we are ready to wait
for the result.
More effort means more reliable results.

We assume that java, cargo and go are installed in /usr/bin.
For dev purposes, that location can be changed in

```
benchrunner/app/src/main/kotlin/benchrunner/App.kt
```

# Methodology

There are many ways we can build a benchmark.
Here are a few things that describe the benchmarks we have here (TLDR, they are DIY microbenchmarks)

- Unlike some other benchmarks, we dont measure the process execution time. Instead, what is
  measured is a loop running a small task many times.
- Each task is run many times before we start measuring it. We call that the warmup.
- We try to target only the cpu efficiency. Each task uses little memory, and no IO.
- These tasks cannot be optimized away by compilers since 1) they operate on nondeterministic data
  2) their result is eventually printed in stdout 3) each task iteration work on different data.
- All tasks compute a nonsensical number that means nothing.
- These nonsensical numbers must be the same for the different implementation of a task, or the
  benchmark fails.
- The GC is run before starting the measurements in kotlin and go.

export LANG_BENCH_MULTIPLIER=10