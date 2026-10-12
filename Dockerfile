# syntax=docker/dockerfile:1

# Stage 1: build the frontend.
FROM node:24.21.0-alpine3.24 AS frontend
WORKDIR /workspace/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/ ./
RUN npm run build

# Stage 2: compile the backend, with the frontend inside, to a native binary.
FROM ghcr.io/graalvm/native-image-community:25i4-25.0.4.1.1-ol10 AS backend
WORKDIR /workspace
COPY mvnw ./
COPY .mvn/ .mvn/
COPY backend/pom.xml backend/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q -f backend -Pnative dependency:go-offline
COPY backend/src/ backend/src/
COPY --from=frontend /workspace/frontend/dist/ backend/src/main/resources/static/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q -f backend -Pnative -DskipTests native:compile \
    && mkdir -p /workspace/data

# Stage 3: minimal runtime, non-root, only the binary.
FROM gcr.io/distroless/base-debian13:nonroot@sha256:a0d70d6a97cd697d9362bc2aae4a6560dd65817e365d0043b07325a97975dc91
COPY --from=backend /workspace/backend/target/prontuario-unico /app/prontuario-unico
COPY --from=backend --chown=65532:65532 /workspace/data /data
USER nonroot
ENV SERVER_ADDRESS=0.0.0.0 \
    DATA_DIR=/data
EXPOSE 8080
ENTRYPOINT ["/app/prontuario-unico", "-Xmx64m"]
