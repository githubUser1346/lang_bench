FROM ubuntu:23.04

RUN apt-get update
RUN apt-get -qq install openjdk-19-jre-headless
RUN apt-get -qq install cargo
RUN apt-get -qq install golang
RUN apt-get -qq install git
RUN echo hd83hd93rg
RUN git clone https://github.com/githubUser1346/lang_bench.git
WORKDIR /lang_bench/benchrunner
# RUN git checkout dev
# The first compilation is slow and downloads a bunch of things in the image.
RUN bash gradlew run

CMD bash docker.sh