# Send Traffic

![GitHub issues](https://img.shields.io/github/issues/dayvidwhy/send-traffic)
![GitHub pull requests](https://img.shields.io/github/issues-pr/dayvidwhy/send-traffic)
![GitHub](https://img.shields.io/github/license/dayvidwhy/send-traffic)

Load testing as a service.

## Prerequisites

Before you begin, ensure you have the following installed:
- Docker
- Git

## Getting Started

The development environment is provided by containers.

```bash
git clone git@github.com:dayvidwhy/send-traffic.git
cd send-traffic
docker-compose up --build
docker exec -it send-traffic-app bash
```

Copy the example env file and update the variables.

```bash
cp .env.example .env
```

Build and run the project.

```bash
gradle build
gradle run
```

Server will be available at `localhost:8080` on your machine.

## VSCode Integration
For an optimized development experience, attach VSCode to the running send-traffic-app container:

1. Use the command palette (Ctrl+Shift+P or Cmd+Shift+P on Mac) and select: `>Dev Containers: Attach to Running Container...`
2. Choose /send-traffic-app from the list.

The Kotlin language extension is defined in `./.vscode/devcontainer.json`.

The language server will start as VSCode attaches.
