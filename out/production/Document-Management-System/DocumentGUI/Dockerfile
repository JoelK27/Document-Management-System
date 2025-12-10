FROM nginx:alpine

# Arbeitsverzeichnis setzen
WORKDIR /usr/share/nginx/html

# Kopiere alle Dateien mit flacher Struktur
COPY src/ ./
COPY *.html ./

# Stelle sicher, dass FrontPage.html als index.html verfügbar ist
RUN cp FrontPage.html index.html

# Kopiere die nginx.conf
COPY nginx.conf /etc/nginx/nginx.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]