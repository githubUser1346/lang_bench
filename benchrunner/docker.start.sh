# This file will at every startup of the container.

cd /lang_bench/benchrunner
# We dont build a super important release image, so we can do that pull. Thanks.
git pull
# This benchrunner app will also rebuild the benchmarks, to account for the pull we just did,
# although the builds made while building the image will have downloaded the bulk of the dependencies.
./gradlew run