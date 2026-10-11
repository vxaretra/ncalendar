# NCalendar

A desktop calendar built with Kotlin and Compose Multiplatform — with a built-in AI assistant that can manage your schedule for you.

## Features

- **Adaptive layout** — switches between side-by-side and stacked based on window width
- Create, edit, and delete events: title, date, start/end time, description
- Background scheduler fires reminders on time while the app is open
- AI agents that powers lists, creates, updates, and deletes events through natural language

## Tech stack

- Kotlin 2.4 
- Compose Multiplatform 1.12 
- SQLDelight 
- Koog 1.3 

## Getting started

1. Set `OPENROUTER_API_KEY` environment variable

2. Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and
options:

- Desktop app:
    - Hot reload: `./gradlew :desktopApp:hotRun --auto`
    - Standard run: `./gradlew :desktopApp:run`
