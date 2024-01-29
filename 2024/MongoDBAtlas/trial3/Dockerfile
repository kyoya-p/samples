FROM ubuntu:22.04
RUN apt update
RUN apt install -y git g++ cmake
RUN apt install -y zlib1g-dev libssl-dev libcurl4-openssl-dev

WORKDIR /opt
COPY CMakeLists.txt .
COPY main.cpp .
#RUN apt install -y openssl
RUN cmake . -B build

WORKDIR /opt/build
RUN cmake --build .

