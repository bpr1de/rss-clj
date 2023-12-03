# Build the jar (default platform)
FROM clojure:temurin-21-lein AS builder
COPY . /build
WORKDIR /build
RUN lein uberjar

# Run the jar
# This assumes you will bind-mount your config file in /app/rss.xml
FROM clojure:temurin-21-lein
RUN mkdir /app
COPY --from=builder /build/target/uberjar/rss-*-standalone.jar /app/rss.jar
CMD ["java", "-jar", "/app/rss.jar"]
