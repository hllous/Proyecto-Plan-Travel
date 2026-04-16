# Plan Travel MVP

MVP Android app built with Kotlin, Jetpack Compose, Room, and Hilt.

## Implemented in this stage

- Travel groups with roles (admin/user)
- Member invitations via code, link, and QR
- In-app QR camera scan flow
- Argentina destination recommendations by region
- BallRoom expense split by consumed item quantity per member
- Leftover amount assigned to group admin

## Architecture

- Clean Architecture style:
  - `domain`: models, repository contract, use cases
  - `data`: Room entities/DAO/database + repository implementation
  - `presentation`: ViewModel + Compose UI
  - `di`: Hilt modules

## Run

Use Android Studio to sync and run `app` on an emulator/device.

