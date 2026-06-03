package com.pelock.jobfuscator.maven;

import com.pelock.jobfuscator.JObfuscatorClient;
import com.pelock.jobfuscator.JObfuscatorResult;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(
    name = "obfuscate-sources",
    defaultPhase = LifecyclePhase.GENERATE_SOURCES,
    threadSafe = true,
    requiresOnline = true,
    inheritByDefault = false)
public class ObfuscateSourcesMojo extends AbstractMojo {

  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject project;

  @Parameter(property = "jobfuscator.skip", defaultValue = "false")
  private boolean skip;

  @Parameter(property = "jobfuscator.failOnError", defaultValue = "true")
  private boolean failOnError;

  @Parameter(property = "jobfuscator.replaceMainCompileSources", defaultValue = "true")
  private boolean replaceMainCompileSources;

  @Parameter(property = "jobfuscator.threads", defaultValue = "4")
  private int threads;

  @Parameter(property = "jobfuscator.apiKey")
  private String apiKey;

  @Parameter(defaultValue = "${project.basedir}/src/main/java", required = true)
  private File inputDirectory;

  @Parameter(
      defaultValue = "${project.build.directory}/generated-sources/jobfuscator",
      required = true)
  private File outputDirectory;

  @Parameter(defaultValue = "true")
  private boolean enableCompression;

  @Parameter(defaultValue = "true")
  private boolean mixCodeFlow;

  @Parameter(defaultValue = "true")
  private boolean renameVariables;

  @Parameter(defaultValue = "true")
  private boolean renameMethods;

  @Parameter(defaultValue = "true")
  private boolean shuffleMethods;

  @Parameter(defaultValue = "true")
  private boolean intsMathCrypt;

  @Parameter(defaultValue = "true")
  private boolean cryptStrings;

  @Parameter(defaultValue = "true")
  private boolean stringSplit;

  @Parameter(defaultValue = "true")
  private boolean intsToArrays;

  @Parameter(defaultValue = "true")
  private boolean dblsToArrays;

  @Parameter(defaultValue = "true")
  private boolean dblsMathCrypt;

  @Parameter(defaultValue = "true")
  private boolean stringCharVault;

  @Parameter(defaultValue = "true")
  private boolean intsFromDoubleMath;

  @Parameter(defaultValue = "true")
  private boolean opaqueMixerChain;

  @Parameter(defaultValue = "true")
  private boolean complexifyBooleans;

  @Parameter(defaultValue = "true")
  private boolean tryFinallyNoise;

  @Parameter(defaultValue = "true")
  private boolean selfCheck;

  @Parameter(defaultValue = "true")
  private boolean arrayIntCrypt;

  @Parameter(defaultValue = "true")
  private boolean arrayCharCrypt;

  @Parameter(defaultValue = "true")
  private boolean arrayDoubleCrypt;

  @Parameter(defaultValue = "true")
  private boolean arrayStringCrypt;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (skip) {
      getLog().info("jobfuscator.skip is set — skipping source obfuscation.");
      return;
    }
    if (!inputDirectory.isDirectory()) {
      throw new MojoExecutionException("inputDirectory is not a directory: " + inputDirectory);
    }

    Path inputRoot = inputDirectory.toPath().normalize().toAbsolutePath();
    Path outputRoot = outputDirectory.toPath().normalize().toAbsolutePath();

    ObfuscatePathValidator.validate(inputRoot, outputRoot);
    warnIfInputNotOnCompileClasspath(inputRoot);

    try {
      prepareOutputDirectory(outputRoot);
    } catch (IOException e) {
      throw new MojoExecutionException("Could not prepare output directory", e);
    }

    JObfuscatorClient client = newClient();

    List<Path> relatives;
    try (Stream<Path> walk = Files.walk(inputRoot)) {
      relatives =
          walk.filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".java"))
              .map(inputRoot::relativize)
              .sorted()
              .collect(Collectors.toList());
    } catch (IOException e) {
      throw new MojoExecutionException("Could not scan inputDirectory", e);
    }

    if (relatives.isEmpty()) {
      getLog().info("No Java sources under " + inputRoot + " — nothing to obfuscate.");
      adjustCompileSourceRoots(inputRoot, outputRoot);
      return;
    }

    int effectiveThreads = threads < 1 ? 1 : threads;
    if (threads < 1) {
      getLog().warn("jobfuscator.threads < 1 is invalid; using 1.");
    }

    List<Path> failures = new ArrayList<>();
    List<JObfuscatorResult> obfuscateResults = new ArrayList<>(relatives.size());
    int poolSize = Math.min(effectiveThreads, relatives.size());
    if (poolSize <= 1) {
      for (Path rel : relatives) {
        try {
          obfuscateResults.add(obfuscateOne(client, inputRoot, outputRoot, rel));
        } catch (IOException | MojoExecutionException e) {
          handleFailure(rel, e, failures);
        }
      }
    } else {
      getLog().info("Obfuscating " + relatives.size() + " file(s) using " + poolSize + " threads.");
      AtomicInteger seq = new AtomicInteger();
      ExecutorService exec =
          Executors.newFixedThreadPool(poolSize, r -> daemonThread(r, seq.getAndIncrement()));
      List<Future<JObfuscatorResult>> futures = new ArrayList<>(relatives.size());
      try {
        for (int j = 0; j < relatives.size(); j++) {
          final int idx = j;
          futures.add(
              exec.submit(
                  () -> obfuscateOne(client, inputRoot, outputRoot, relatives.get(idx))));
        }
        for (int i = 0; i < futures.size(); i++) {
          Path rel = relatives.get(i);
          Future<JObfuscatorResult> future = futures.get(i);
          try {
            obfuscateResults.add(future.get());
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            exec.shutdownNow();
            throw new MojoExecutionException("Interrupted during obfuscation", ie);
          } catch (ExecutionException ee) {
            Throwable cause = ee.getCause();
            IOException io =
                cause instanceof IOException ? (IOException) cause : null;
            MojoExecutionException mex =
                cause instanceof MojoExecutionException
                    ? (MojoExecutionException) cause
                    : null;
            failures.add(rel);
            if (failOnError) {
              exec.shutdownNow();
              if (mex != null) {
                throw mex;
              }
              if (io != null) {
                throw new MojoExecutionException("Failed obfuscating " + rel + ": " + io.getMessage(), io);
              }
              throw new MojoExecutionException(
                  "Failed obfuscating " + rel + ": " + cause.getMessage(), cause);
            }
            getLog().warn("Skipping " + rel + ": " + cause.getMessage());
          }
        }
      } finally {
        exec.shutdown();
        try {
          if (!exec.awaitTermination(4, TimeUnit.HOURS)) {
            exec.shutdownNow();
          }
        } catch (InterruptedException ie) {
          exec.shutdownNow();
          Thread.currentThread().interrupt();
        }
      }
    }

    if (!failures.isEmpty() && failOnError) {
      throw new MojoFailureException(
          "Obfuscation failed for " + failures.size() + " file(s), first: " + failures.get(0));
    }
    if (!failures.isEmpty()) {
      getLog().warn("Obfuscation skipped or failed for " + failures.size() + " file(s).");
    }

    adjustCompileSourceRoots(inputRoot, outputRoot);
    ObfuscateLicenseSummary licenseSummary = ObfuscateLicenseSummary.fromResults(obfuscateResults);
    getLog()
        .info(
            ObfuscateLicenseSummary.formatProcessedMessage(
                relatives.size() - failures.size(), licenseSummary));
  }

  private static Thread daemonThread(Runnable r, int id) {
    Thread t = new Thread(r, "jobfuscator-" + id);
    t.setDaemon(true);
    return t;
  }

  private void handleFailure(Path rel, Exception e, List<Path> failures)
      throws MojoExecutionException {
    failures.add(rel);
    if (failOnError) {
      if (e instanceof MojoExecutionException) {
        throw (MojoExecutionException) e;
      }
      throw new MojoExecutionException("Failed obfuscating " + rel + ": " + e.getMessage(), e);
    }
    getLog().warn("Skipping " + rel + ": " + e.getMessage());
  }

  private JObfuscatorResult obfuscateOne(
      JObfuscatorClient client, Path inputRoot, Path outputRoot, Path rel)
      throws IOException, MojoExecutionException {
    Path src = inputRoot.resolve(rel);
    Path dst = outputRoot.resolve(rel);
    Files.createDirectories(dst.getParent());

    String content = Files.readString(src);
    JObfuscatorResult result = client.obfuscateJavaSource(content);
    if (!result.isSuccess()) {
      throw new MojoExecutionException(
          "JObfuscator API error " + result.getError() + " for " + rel);
    }
    String out = result.getOutput();
    if (out == null) {
      throw new MojoExecutionException("Empty output from API for " + rel);
    }
    Files.writeString(dst, out);
    getLog().debug("Obfuscated " + rel);
    return result;
  }

  /**
   * Logs when {@code inputDirectory} is not one of {@link MavenProject#getCompileSourceRoots()},
   * since the mojo obfuscates every {@code *.java} under {@code inputDirectory} — not every compile
   * root by default.
   */
  private void warnIfInputNotOnCompileClasspath(Path inputRoot) {
    try {
      for (String root : project.getCompileSourceRoots()) {
        Path p = Path.of(root).normalize().toAbsolutePath();
        if (sameRoot(p, inputRoot)) {
          return;
        }
      }
      getLog().warn(
          "jobfuscator inputDirectory ("
              + inputRoot
              + ") does not match any entry in project.getCompileSourceRoots() "
              + project.getCompileSourceRoots()
              + ". By default this should be your main Java sources directory so obfuscation "
              + "matches what javac compiles; adjust inputDirectory or compile source roots.");
    } catch (IOException e) {
      getLog().debug("Could not compare inputDirectory to compile source roots: " + e.getMessage());
    }
  }

  private void prepareOutputDirectory(Path outputRoot) throws IOException {
    if (!Files.exists(outputRoot)) {
      Files.createDirectories(outputRoot);
      return;
    }
    try (Stream<Path> walk = Files.walk(outputRoot)) {
      List<Path> paths = walk.sorted(Comparator.reverseOrder()).collect(Collectors.toList());
      for (Path p : paths) {
        Files.deleteIfExists(p);
      }
    }
    Files.createDirectories(outputRoot);
  }

  private void adjustCompileSourceRoots(Path inputRoot, Path outputRoot)
      throws MojoExecutionException {
    if (!replaceMainCompileSources) {
      project.addCompileSourceRoot(outputDirectory.getAbsolutePath());
      getLog().debug("Added compile source root: " + outputDirectory);
      return;
    }
    try {
      Iterator<String> it = project.getCompileSourceRoots().iterator();
      while (it.hasNext()) {
        String root = it.next();
        Path rootPath = Path.of(root).normalize().toAbsolutePath();
        if (sameRoot(rootPath, inputRoot)) {
          it.remove();
          getLog().debug("Removed compile source root matching inputDirectory: " + root);
        }
      }
    } catch (IOException e) {
      throw new MojoExecutionException("Could not adjust compile source roots", e);
    }
    project.addCompileSourceRoot(outputDirectory.getAbsolutePath());
    getLog().debug("Compile sources now include: " + outputDirectory.getAbsolutePath());
  }

  private boolean sameRoot(Path compileRoot, Path configuredInput) throws IOException {
    if (!Files.exists(configuredInput)) {
      return compileRoot.equals(configuredInput.normalize());
    }
    if (!Files.exists(compileRoot)) {
      return compileRoot.normalize().equals(configuredInput.normalize());
    }
    return Files.isSameFile(compileRoot, configuredInput);
  }

  private JObfuscatorClient newClient() {
    String key = apiKey == null || apiKey.isBlank() ? null : apiKey;
    JObfuscatorClient c = new JObfuscatorClient(key);
    c.enableCompression = enableCompression;
    c.mixCodeFlow = mixCodeFlow;
    c.renameVariables = renameVariables;
    c.renameMethods = renameMethods;
    c.shuffleMethods = shuffleMethods;
    c.intsMathCrypt = intsMathCrypt;
    c.cryptStrings = cryptStrings;
    c.stringSplit = stringSplit;
    c.intsToArrays = intsToArrays;
    c.dblsToArrays = dblsToArrays;
    c.dblsMathCrypt = dblsMathCrypt;
    c.stringCharVault = stringCharVault;
    c.intsFromDoubleMath = intsFromDoubleMath;
    c.opaqueMixerChain = opaqueMixerChain;
    c.complexifyBooleans = complexifyBooleans;
    c.tryFinallyNoise = tryFinallyNoise;
    c.selfCheck = selfCheck;
    c.arrayIntCrypt = arrayIntCrypt;
    c.arrayCharCrypt = arrayCharCrypt;
    c.arrayDoubleCrypt = arrayDoubleCrypt;
    c.arrayStringCrypt = arrayStringCrypt;
    return c;
  }
}
