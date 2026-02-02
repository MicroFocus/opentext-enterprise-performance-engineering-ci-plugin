# ------------------------
# Java build stage
# ------------------------
FROM maven:3.9.10-eclipse-temurin-17 AS java-builder
WORKDIR /build/java-app

# Copy pom and pre-download dependencies
COPY java-app/pom.xml .
RUN mvn dependency:go-offline

# Copy source and build JAR
COPY java-app .
RUN mvn clean package -DskipTests

# ------------------------
# Node build stage
# ------------------------
FROM node:20 AS node-builder
WORKDIR /build/nodejs-app

COPY nodejs-app/package*.json .
RUN npm install

COPY nodejs-app .
RUN npx tsc

# ------------------------
# Java runtime (TLS-safe full JRE)
# ------------------------
FROM eclipse-temurin:17-jre AS jre-builder

# ------------------------
# Runtime image (Node + Java)
# ------------------------
FROM node:20-slim

# ------------------------
# System dependencies
# ------------------------
RUN apt-get update \
 && apt-get install -y ca-certificates \
 && rm -rf /var/lib/apt/lists/*

# ------------------------
# Metadata & Environment
# ------------------------
LABEL maintainer="ddanan@opentext.com"
LABEL version="1.0"
LABEL description="Node.js + Java app for integrating LRE test into Harness pipeline"

ENV JAVA_HOME=/opt/jre
ENV PATH="$JAVA_HOME/bin:$PATH"
ENV NODE_ENV=production
ENV HOME=/home/appuser

WORKDIR /app

# ------------------------
# Copy Java and Node artifacts
# ------------------------
COPY --from=jre-builder /opt/java/openjdk /opt/jre
COPY --from=node-builder /build/nodejs-app/dist /app/dist
COPY --from=java-builder /build/java-app/target/*-jar-with-dependencies.jar /app/dist

# ------------------------
# Harness defaults
# ------------------------
ENV PLUGIN_LRE_OUTPUT_DIR="/harness/output"
ENV PLUGIN_LRE_WORKSPACE_DIR="/harness"

# ------------------------
# Run as non-root user
# ------------------------
RUN useradd -m appuser
USER appuser

# ------------------------
# Entrypoint
# ------------------------
ENTRYPOINT ["node", "/app/dist/app.js"]
