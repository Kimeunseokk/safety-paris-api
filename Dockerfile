# 1단계: 빌드 - Gradle로 실행 가능한 jar 생성 (JDK 필요)
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# 의존성 다운로드를 소스 복사보다 먼저 해서, 소스만 바뀌었을 땐 이 단계를 캐시로 재사용 (재빌드 속도↑)
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN ./gradlew dependencies --no-daemon > /dev/null

COPY src ./src
# 테스트는 MySQL/Redis가 필요해서 이미지 빌드 땐 건너뜀 (테스트는 CI에서 따로 실행)
RUN ./gradlew bootJar -x test --no-daemon

# 2단계: 실행 - jar만 가져와 JRE로 실행 (JDK·Gradle·소스 없이 이미지 크기 최소화)
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
