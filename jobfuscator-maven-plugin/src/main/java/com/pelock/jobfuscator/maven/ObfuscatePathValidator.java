package com.pelock.jobfuscator.maven;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.maven.plugin.MojoExecutionException;

/** Guards against configurations that could delete or overwrite original sources. */
final class ObfuscatePathValidator {

  private ObfuscatePathValidator() {}

  /**
   * Ensures input and output roots are not the same location (including after symbolic link
   * resolution). {@link ObfuscateSourcesMojo} only writes under {@code outputRoot}; originals are
   * read from {@code inputRoot}. Pointing both at the same directory would make {@code
   * prepareOutputDirectory} wipe sources.
   */
  static void validate(Path inputRoot, Path outputRoot) throws MojoExecutionException {
    Path inNorm = inputRoot.normalize().toAbsolutePath();
    Path outNorm = outputRoot.normalize().toAbsolutePath();
    try {
      Path inReal = Files.exists(inNorm) ? inNorm.toRealPath() : inNorm;
      Path outReal = Files.exists(outNorm) ? outNorm.toRealPath() : outNorm;
      if (inReal.equals(outReal)) {
        throw new MojoExecutionException(
            "outputDirectory must not be the same path as inputDirectory "
                + "(the plugin clears output before writing; your sources would be destroyed).");
      }
    } catch (IOException e) {
      throw new MojoExecutionException("Could not validate input/output directories", e);
    }
  }
}
