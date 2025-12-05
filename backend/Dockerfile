# Multi-stage build for Spring Boot
FROM maven:3.9-eclipse-temurin-17 AS build

# Maven 캐시 디렉토리 ARG
ARG MAVEN_CACHE_DIR=/maven-cache

# 작업 디렉토리 설정
WORKDIR /app

# pom.xml 먼저 복사 (의존성 캐싱)
COPY pom.xml .

# 의존성 다운로드 (캐시 활용)
RUN mvn dependency:go-offline -Dmaven.repo.local=${MAVEN_CACHE_DIR}

# 소스 코드 복사
COPY src ./src


# 빌드
RUN mvn clean package -DskipTests -Dmaven.repo.local=${MAVEN_CACHE_DIR}

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 빌드된 JAR 파일 복사
COPY --from=build /app/target/*.jar app.jar

# 포트 노출
EXPOSE 8080

# 헬스체크를 위한 curl 설치
RUN apk add --no-cache curl

# Spring Boot 실행
ENTRYPOINT ["java", "-jar", "app.jar"]