FROM gradle:8.7.0-jdk21

WORKDIR /app
COPY . /app

EXPOSE 8080

# hold the container open
CMD ["tail", "-f", "/dev/null"]
