# syntax=docker/dockerfile:1

FROM node:22-bookworm-slim AS frontend-build
WORKDIR /build
COPY frontend/package.json frontend/package-lock.json ./frontend/
RUN cd frontend && npm ci
COPY frontend ./frontend
COPY src/main/resources ./src/main/resources
WORKDIR /build/frontend
RUN npm run build

FROM eclipse-temurin:25-jdk-noble AS backend-build
WORKDIR /build
COPY pom.xml mvnw mvnw./cmd ./
COPY .mvn .mvn
COPY src ./src
COPY --from=frontend-build /build/src/main/resources/static ./src/main/resources/static
RUN chmod +x mvnw && ./mvnw -B package -DskipTests -Pcheckstyle-skip

FROM eclipse-temurin:25-jre-noble
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app --no-create-home app
COPY --from=backend-build /build/target/*.jar app.jar
USER app
EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
