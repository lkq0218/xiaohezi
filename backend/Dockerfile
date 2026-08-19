FROM node:20-alpine AS frontend-build
WORKDIR /workspace/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
ARG VITE_AUTH_MODE=oidc
ARG VITE_KEYCLOAK_URL=http://localhost:8180
ENV VITE_AUTH_MODE=$VITE_AUTH_MODE VITE_KEYCLOAK_URL=$VITE_KEYCLOAK_URL
RUN npm run build

FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY backend/pom.xml backend/pom.xml
RUN mvn -f backend/pom.xml -B dependency:go-offline
COPY backend/src backend/src
RUN rm -rf backend/src/main/resources/static/*
COPY --from=frontend-build /workspace/frontend/dist/ backend/src/main/resources/static/
RUN mvn -f backend/pom.xml -B clean package

FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 app
WORKDIR /app
COPY --from=build /workspace/backend/target/xiaohezi-backend-*.jar app.jar
USER 10001
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --retries=12 \
  CMD curl --fail --silent http://localhost:8080/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
