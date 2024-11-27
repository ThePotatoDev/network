FROM azul/zulu-openjdk:21-latest
COPY . /app
WORKDIR /app
CMD ["java", "-Xmx512M", "-Xms512M", "-jar", "proxy.jar"]