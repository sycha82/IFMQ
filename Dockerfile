# 멀티 모듈 Maven 프로젝트 — 공유 build 스테이지 1회 + 모듈별 런타임 타겟 4개.
# docker compose build 시 동일한 build 스테이지를 4개 서비스가 캐시 공유한다.
#   docker build --target wcs-app -t ifmq/wcs-app .
#   docker build --target shuttle-wcs -t ifmq/shuttle-wcs .
#   docker build --target rcs-mock -t ifmq/rcs-mock .
#   docker build --target wms-mock -t ifmq/wms-mock .

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# 의존성 캐시 레이어 — pom.xml만 먼저 복사
COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY wcs-app/pom.xml wcs-app/pom.xml
COPY shuttle-wcs/pom.xml shuttle-wcs/pom.xml
COPY rcs-mock/pom.xml rcs-mock/pom.xml
COPY wms-mock/pom.xml wms-mock/pom.xml

COPY common common
COPY wcs-app wcs-app
COPY shuttle-wcs shuttle-wcs
COPY rcs-mock rcs-mock
COPY wms-mock wms-mock

RUN mvn -q install -DskipTests


FROM eclipse-temurin:21-jre AS wcs-app
WORKDIR /app
COPY --from=build /workspace/wcs-app/target/wcs-app-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]


FROM eclipse-temurin:21-jre AS shuttle-wcs
WORKDIR /app
COPY --from=build /workspace/shuttle-wcs/target/shuttle-wcs-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]


FROM eclipse-temurin:21-jre AS rcs-mock
WORKDIR /app
COPY --from=build /workspace/rcs-mock/target/rcs-mock-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]


FROM eclipse-temurin:21-jre AS wms-mock
WORKDIR /app
COPY --from=build /workspace/wms-mock/target/wms-mock-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
