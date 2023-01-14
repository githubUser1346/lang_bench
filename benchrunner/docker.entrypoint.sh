cd /lang_bench
git pull
cd benchrunner
export LANG_BENCH_EFFORT=$1
./gradlew run