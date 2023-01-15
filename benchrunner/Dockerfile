FROM ubuntu:23.04

RUN apt-get update
RUN apt-get -qq install openjdk-19-jre-headless
RUN apt-get -qq install cargo
RUN apt-get -qq install golang
RUN apt-get -qq install git

RUN git clone https://github.com/simon1514/lang_bench.git

WORKDIR /lang_bench/rust
RUN cargo build --release

WORKDIR /lang_bench/kotlin-jvm
RUN bash gradlew build

WORKDIR /lang_bench/golang
RUN go build

WORKDIR /lang_bench/benchrunner
RUN bash gradlew build

COPY docker.start.sh docker.start.new.sh
CMD bash docker.start.new.sh