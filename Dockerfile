# Build the jar (default platform)
FROM clojure:temurin-11-lein AS builder
COPY . /build
WORKDIR /build
RUN lein uberjar

# Run the jar (amd64 platform)
# This assumes you will bind-mount your config file in /app/rss.xml
FROM amd64/clojure:temurin-11-alpine
RUN mkdir /app
COPY --from=builder /build/target/uberjar/rss-*-standalone.jar /app/rss.jar
CMD ["java", "--illegal-access=deny", "-jar", "/app/rss.jar", "/app/rss.xml"]
