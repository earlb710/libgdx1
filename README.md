# libgdx1
A Java Libgdx game framework to see what is possible.

## About
This project is a game framework built using libGDX, a cross-platform game development framework for Java.

## Project Structure
- **core**: Contains the main game logic and code that is shared across all platforms
- **desktop**: Contains the desktop-specific launcher and configuration

## Requirements
- Java 17 or higher
- Gradle (included via wrapper)

## Building the Project
To build the project, run:
```bash
./gradlew build
```

## Running the Game
To run the desktop version:
```bash
./gradlew desktop:run
```

## Creating a Distribution
To create a distributable JAR file:
```bash
./gradlew desktop:dist
```
The JAR file will be located in `desktop/build/libs/`

## IDE Setup
### IntelliJ IDEA
1. Import the project as a Gradle project
2. Select the root `build.gradle` file
3. Run the desktop configuration

### Eclipse
1. Run `./gradlew eclipse` to generate Eclipse project files
2. Import as existing project

## Resources
- [libGDX Documentation](https://libgdx.com/dev/)
- [libGDX Wiki](https://libgdx.com/wiki/)

