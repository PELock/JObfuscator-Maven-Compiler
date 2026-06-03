package com.pelock.jobfuscator.maven;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ObfuscatePathValidatorTest {

  @Test
  void rejectsIdenticalInputAndOutputPaths(@TempDir Path tmp) throws Exception {
    Path src = tmp.resolve("src");
    Files.createDirectories(src);
    Path input = src.toRealPath();
    Path output = src.toRealPath();
    assertThrows(MojoExecutionException.class, () -> ObfuscatePathValidator.validate(input, output));
  }

  @Test
  void allowsDistinctPaths(@TempDir Path tmp) throws Exception {
    Path in = Files.createDirectories(tmp.resolve("in")).toRealPath();
    Path out = Files.createDirectories(tmp.resolve("out")).toRealPath();
    ObfuscatePathValidator.validate(in, out);
  }
}
