FROM openjdk:17-jdk-slim

WORKDIR /app

COPY build/libs/BANK-0.0.1-SNAPSHOT.jar /app/BANK.jar

EXPOSE 8080

CMD ["java", "-jar", "BANK.jar"]