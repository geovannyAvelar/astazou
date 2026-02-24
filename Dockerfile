FROM gradle:jdk25 AS builder
WORKDIR /home/gradle/project

COPY --chown=gradle:gradle . .
RUN ./gradlew --no-daemon clean bootJar

FROM eclipse-temurin:25-jre
WORKDIR /app

RUN apt-get update && apt-get install -y python3 python3-venv python3-pip && rm -rf /var/lib/apt/lists/*
RUN ln -s /usr/bin/python3 /usr/bin/python || true

COPY scripts/python/parse_itau_history_pdf.py /app/scripts/parse_itau_history_pdf.py
COPY scripts/python/requirements.txt /app/requirements.txt

RUN python3 -m venv /opt/venv \
    && /opt/venv/bin/pip install --upgrade pip setuptools wheel \
    && /opt/venv/bin/pip install -r /app/requirements.txt \
    && chmod +x /app/scripts/parse_itau_history_pdf.py

COPY --from=builder /home/gradle/project/build/libs/*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]