package org.hammer.audio;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ArchitectureBoundaryTest {

  private static final Path REPOSITORY_ROOT = Path.of("..").toAbsolutePath().normalize();
  private static final List<String> STABLE_MODULES =
      List.of("audio-core", "audio-geometry", "audio-acquisition", "audio-dsp");

  @Test
  void stableAudioPackagesDoNotDependOnExperimentalPackages() throws IOException {
    List<String> violations = new ArrayList<>();
    for (String module : STABLE_MODULES) {
      try (Stream<Path> files = Files.walk(mainJava(module))) {
        files
            .filter(path -> path.toString().endsWith(".java"))
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
    }
    assertNoViolations(violations);
  }

  @Test
  void stableModulesDoNotDependOnUiOrAppPackages() throws IOException {
    List<String> violations = new ArrayList<>();
    for (String module : STABLE_MODULES) {
      try (Stream<Path> files = Files.walk(mainJava(module))) {
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

  @Test
  void modulePomDependenciesPreserveStableBoundaries() throws IOException {
    List<String> violations = new ArrayList<>();
    for (String module : STABLE_MODULES) {
      String pom = Files.readString(REPOSITORY_ROOT.resolve(module).resolve("pom.xml"));
      if (pom.contains("<artifactId>audio-app</artifactId>")
          || pom.contains("<artifactId>audio-experimental-acoustic</artifactId>")) {
        violations.add(module + " must not depend on app or experimental modules");
      }
    }
    String appPom = Files.readString(REPOSITORY_ROOT.resolve("audio-app/pom.xml"));
    if (appPom.contains("<artifactId>audio-experimental-acoustic</artifactId>")) {
      violations.add("audio-app must not require audio-experimental-acoustic");
    }
    String experimentalPom =
        Files.readString(REPOSITORY_ROOT.resolve("audio-experimental-acoustic/pom.xml"));
    for (String artifactId : dependencyArtifactIds(experimentalPom)) {
      if (artifactId.startsWith("audio-") && !stableArtifactIds().contains(artifactId)) {
        violations.add("audio-experimental-acoustic has non-stable dependency: " + artifactId);
      }
    }
    assertNoViolations(violations);
  }

  private static Path mainJava(String module) {
    return REPOSITORY_ROOT.resolve(module).resolve("src/main/java");
  }

  private static Set<String> stableArtifactIds() {
    return Set.of("audio-core", "audio-geometry", "audio-acquisition", "audio-dsp");
  }

  private static List<String> dependencyArtifactIds(String pom) {
    List<String> artifactIds = new ArrayList<>();
    boolean inDependency = false;
    for (String line : pom.lines().toList()) {
      String trimmed = line.trim();
      if ("<dependency>".equals(trimmed)) {
        inDependency = true;
      } else if ("</dependency>".equals(trimmed)) {
        inDependency = false;
      } else if (inDependency
          && trimmed.startsWith("<artifactId>")
          && trimmed.endsWith("</artifactId>")) {
        artifactIds.add(trimmed.replace("<artifactId>", "").replace("</artifactId>", ""));
      }
    }
    return artifactIds;
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
