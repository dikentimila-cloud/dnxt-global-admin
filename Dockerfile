FROM eclipse-temurin:21-jre
LABEL maintainer="DNXT Solutions"
LABEL description="DnXT Global Admin Portal - Platform Administration"

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
RUN groupadd -r dnxt && useradd -r -g dnxt dnxt

COPY target/global-admin.jar app.jar

RUN mkdir -p /app/data && chown -R dnxt:dnxt /app
USER dnxt

EXPOSE 8110

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
