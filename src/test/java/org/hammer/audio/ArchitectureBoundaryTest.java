package org.hammer.audio;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ArchitectureBoundaryTest {

  private static final Path MAIN_JAVA = Path.of("src/main/java");

  @Test
  void stableAudioPackagesDoNotDependOnExperimentalPackages() throws IOException {
    List<String> violations = new ArrayList<>();
    try (Stream<Path> files = Files.walk(MAIN_JAVA.resolve("org/hammer/audio"))) {
      files
          .filter(path -> path.toString().endsWith(".java"))
          .filter(path -> !path.toString().contains("/experimental/"))
          .forEach(
              path ->
                  importLines(path).stream()
                      .forEach(
                          line -> {
                            if (line.contains("import org.hammer.audio.experimental.")) {
                              violations.add(path + ": " + line.trim());
                            }
                          }));
    }
    assertNoViolations(violations);
  }

  @Test
  void dspAcquisitionAndGeometryDoNotDependOnUiOrAppPackages() throws IOException {
    List<String> violations = new ArrayList<>();
    for (String packagePath : List.of("dsp", "acquisition", "geometry")) {
      try (Stream<Path> files = Files.walk(MAIN_JAVA.resolve("org/hammer/audio").resolve(packagePath))) {
        files
            .filter(path -> path.toString().endsWith(".java"))
            .forEach(
                path ->
                    importLines(path).stream()
                        .forEach(
                            line -> {
                              String trimmed = line.trim();
                              if (trimmed.startsWith("import org.hammer.audio.ui.")
                                  || trimmed.matches("import org\\.hammer\\.(?!audio\\.).*")) {
                                violations.add(path + ": " + trimmed);
                              }
                            }));
      }
    }
    assertNoViolations(violations);
  }

  private static List<String> importLines(Path path) {
    try {
      return Files.readAllLines(path).stream()
          .filter(line -> line.trim().startsWith("import "))
          .toList();
    } catch (IOException ex) {
      throw new IllegalStateException("Cannot read " + path, ex);
    }
  }

  private static void assertNoViolations(List<String> violations) {
    if (!violations.isEmpty()) {
      fail("Architecture boundary violations:\n" + String.join("\n", violations));
    }
  }
}
