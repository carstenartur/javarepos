# javarepos - AudioIn Audio Analyzer

An Eclipse/OSGi-based audio analyzer application built with Tycho.

## Project Structure

This project uses Eclipse Tycho for building OSGi bundles and features:

- **audioin.bundle** - Eclipse plugin containing the audio analyzer application (Java sources in `org.hammer` package)
- **audioin.feature** - Eclipse feature that groups the audioin.bundle plugin
- **audioin.repository** - P2 update site for Eclipse installation
- **audioin.target** - Target platform definition pointing to Eclipse 2024-03 release

## Target Platform

The project uses a dedicated `.target` file (`audioin.target/audioin.target`) that references Eclipse SimRel 2024-03:
- Primary repository: https://download.eclipse.org/releases/2024-03
- Includes: `org.eclipse.platform.feature.group` and `org.eclipse.equinox.executable.feature.group`

**Note:** The project uses Eclipse 2024-03 instead of 2025-06 because Java 17 compatibility is required, and Eclipse 2025-06 requires Java 21. Eclipse 2024-03 is the latest stable release fully compatible with Java 17.

## Building

### Prerequisites
- JDK 17
- Maven 3.6+

### Build Command
```bash
mvn -U -e -V clean verify
```

This will:
1. Resolve dependencies from the target platform
2. Compile the bundle sources
3. Create the feature
4. Build the P2 repository under `audioin.repository/target/repository/`

### Build Artifacts
After a successful build, you'll find:
- Plugin JAR: `audioin.bundle/target/audioin.bundle-0.0.1-SNAPSHOT.jar`
- Feature JAR: `audioin.feature/target/audioin.feature-0.0.1-SNAPSHOT.jar`
- P2 Repository: `audioin.repository/target/repository/`
- Repository ZIP: `audioin.repository/target/audioin.repository-0.0.1-SNAPSHOT.zip`

## Installing into Eclipse

1. Build the project as described above
2. In Eclipse, go to **Help → Install New Software...**
3. Click **Add...** and then **Local...**
4. Browse to `audioin.repository/target/repository`
5. Select the "Audioin" category
6. Click **Next** and complete the installation wizard

## Technology Stack

- **Java**: 17
- **Build System**: Apache Maven with Eclipse Tycho 4.0.6
- **Target Platform**: Eclipse 2024-03 (SimRel)
- **OSGi**: Eclipse Equinox
- **UI**: AWT/Swing (no Eclipse UI dependencies)

## Application Details

The audioin.bundle contains a pure AWT/Swing audio analyzer application with the following features:
- Real-time audio input capture
- Waveform visualization
- Phase diagram display
- Audio format configuration

Main entry point: `org.hammer.AudioAnalyseFrame`