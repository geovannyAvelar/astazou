FROM ghcr.io/graalvm/graalvm-community:25 AS builder
WORKDIR /home/gradle/project

COPY . .
RUN ./gradlew --no-daemon clean bootJar

FROM ghcr.io/graalvm/graalvm-community:25
WORKDIR /app

RUN microdnf install -y python3 python3-pip && microdnf clean all
RUN ln -s /usr/bin/python3 /usr/bin/python || true

COPY scripts/python/parse_itau_history_pdf.py /app/scripts/parse_itau_history_pdf.py
COPY scripts/python/requirements.txt /app/requirements.txt

RUN python3 -m venv /opt/venv \
    && /opt/venv/bin/pip install --upgrade pip setuptools wheel \
    && /opt/venv/bin/pip install -r /app/requirements.txt \
    && chmod +x /app/scripts/parse_itau_history_pdf.py

COPY --from=builder /home/gradle/project/build/libs/*.jar /app/app.jar

RUN useradd -m appuser && chown -R appuser:appuser /app /opt/venv

USER appuser

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]