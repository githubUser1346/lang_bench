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

# Sample Result

```
Benchmark Summary (Smaller percentage is better) (Ignored 2 warmups) (effort=400) 
  nonVectoLoop                     scale:  1773ms
    go                               mean: 82%, meanDiff:  1%
    kotlin-jvm                       mean: 84%, meanDiff:  5%
    rust                             mean:100%, meanDiff:  1%
  branchingNonVectoLoop            scale:  3789ms
    rust                             mean: 39%, meanDiff:  0%
    go                               mean: 69%, meanDiff:  1%
    kotlin-jvm                       mean:100%, meanDiff: 78%
  complexAutoVectoLoop             scale:   931ms
    rust                             mean: 64%, meanDiff:  1%
    kotlin-jvm                       mean: 79%, meanDiff:  2%
    go                               mean:100%, meanDiff:  1%
  trivialAutoVectoLoop             scale: 12689ms
    rust                             mean: 12%, meanDiff:  0%
    kotlin-jvm                       mean: 50%, meanDiff:  0%
    go                               mean:100%, meanDiff:  1%
  branchingAutoVectoLoop           scale: 19131ms
    rust                             mean: 14%, meanDiff:  0%
    go                               mean: 76%, meanDiff:  0%
    kotlin-jvm                       mean:100%, meanDiff: 34%
  itoa                             scale: 17550ms
    rust                             mean: 19%, meanDiff:  0%
    kotlin-jvm-heapless              mean: 45%, meanDiff:  2%
    kotlin-jvm                       mean: 45%, meanDiff:  0%
    go                               mean:100%, meanDiff:  1%
  json-ser                         scale:  2365ms
    rust-serde                       mean: 27%, meanDiff:  0%
    kotlin-jvm-jsoniter              mean: 89%, meanDiff:  1%
    go-jsoniter                      mean:100%, meanDiff:  2%
  json-deser                       scale:  4720ms
    rust-serde                       mean: 44%, meanDiff:  0%
    kotlin-jvm-jsoniter              mean: 53%, meanDiff:  1%
    go-jsoniter                      mean:100%, meanDiff:  1%

```