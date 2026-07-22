# Contributing

## Reporting Bugs

Open a GitHub Issue with:

- Android version and device model
- BLE adapter information (manufacturer, chipset if known)
- Steps to reproduce
- Full logcat output if available

## Submitting Changes

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/my-change`)
3. Make your changes
4. Build and test locally:
   ```
   JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug
   ```
5. Commit with a clear message
6. Open a Pull Request

## Code Style

- Java, follow existing patterns in the codebase
- No comments that explain what the code does (code should be self-documenting)
- Comments explain why, not what
- Keep methods short and focused

## Documentation

- Doc changes belong in the same PR as the code changes they describe
- Follow [docs/DOCUMENTATION_GUIDE.md](docs/DOCUMENTATION_GUIDE.md) for documentation standards
- See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) to understand the project structure
