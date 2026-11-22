FROM ghcr.io/graalvm/native-image-community:25-muslib AS builder
WORKDIR /app

COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN sh ./mvnw --batch-mode dependency:go-offline

COPY src ./src
RUN sh ./mvnw --batch-mode clean native:compile -Pnative -DskipTests \
    -Dspring-boot.aot.profiles=prod

FROM alpine:latest
WORKDIR /app

RUN apk add --no-cache ca-certificates tzdata \
    && addgroup -g 1001 -S appgroup \
    && adduser -u 1001 -S appuser -G appgroup

COPY --from=builder --chown=appuser:appgroup /app/target/groups ./app

USER appuser
EXPOSE 6598
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["./app"]
