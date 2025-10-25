# --- Build Stage ---
FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -q clean package -DskipTests

# --- Runtime Stage (Ubuntu Noble) ---
FROM eclipse-temurin:21-jre-noble
WORKDIR /app

# Tesseract + Sprachen + Dev-Libs
RUN apt-get update \
  && apt-get install -y --no-install-recommends \
     tesseract-ocr tesseract-ocr-deu tesseract-ocr-eng \
     libleptonica-dev libtesseract-dev \
  && rm -rf /var/lib/apt/lists/*

# Optionaler Kompatibilitäts-Symlink
RUN ln -sf /usr/share/tesseract-ocr/5/tessdata /usr/share/tesseract-ocr/tessdata

# OCR-Konfiguration
ENV OCR_LANG=deu+eng

COPY --from=build /workspace/target/ocr-worker.jar /app/app.jar

ENTRYPOINT ["java","-jar","/app/app.jar"]
