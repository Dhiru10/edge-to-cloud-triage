FROM ubuntu:22.04 AS build

RUN apt-get update && apt-get install -y --no-install-recommends \
    cmake g++ libcurl4-openssl-dev ca-certificates git \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY CMakeLists.txt .
COPY include/ ./include/
COPY src/ ./src/

RUN cmake -B build -DCMAKE_BUILD_TYPE=Release && \
    cmake --build build --parallel $(nproc)

FROM ubuntu:22.04
RUN apt-get update && apt-get install -y --no-install-recommends \
    libcurl4 && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /app/build/edge-client .
ENTRYPOINT ["./edge-client"]
