# Docker setup

Build and run Gradle tasks inside a container with the Android SDK pinned to API 36.

```
docker compose build
docker compose run --rm android ./gradlew assembleDebug
docker compose run --rm android ./gradlew test
```
