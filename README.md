# JObfuscator — Maven plugin for Java source obfuscation

**JObfuscator** is a Java **source** obfuscator: it transforms `.java` files into a protected form while preserving behaviour. It applies renaming, non-linear control flow, string encryption, and many other strategies ([product overview](https://www.pelock.com/products/jobfuscator)).

This repository provides:

| Artifact | Role |
|----------|------|
| [jobfuscator-maven-plugin](https://central.sonatype.com/artifact/com.pelock/jobfuscator-maven-plugin) | Maven goal `jobfuscator:obfuscate-sources` (runs in `generate-sources`) |
| [jobfuscator-client](https://central.sonatype.com/artifact/com.pelock/jobfuscator-client) | Standalone Java client for the JObfuscator Web API |

More documentation, downloads, and APIs:

- [JObfuscator product page](https://www.pelock.com/products/jobfuscator)
- [API / integrations](https://www.pelock.com/products/jobfuscator/api)
- [Online Java obfuscator](https://www.pelock.com/jobfuscator/)

---

## Why obfuscate Java?

Compiled Java (`.class`, `.jar`, `.war`) is **bytecode** that decompiles cleanly back to readable Java. Obfuscation makes recovered source harder to analyse while keeping your application runnable.

---

## Installation (Maven)

You can find [jobfuscator-maven-plugin](https://central.sonatype.com/artifact/com.pelock/jobfuscator-maven-plugin) at Maven Central under **`com.pelock`**:

Add the plugin to your application `pom.xml`:

```xml
<plugin>
  <groupId>com.pelock</groupId>
  <artifactId>jobfuscator-maven-plugin</artifactId>
  <version><!-- latest released version --></version>
</plugin>
```

Optional property for version alignment:

```xml
<properties>
  <jobfuscator.version><!-- e.g. 1.0.2 --></jobfuscator.version>
</properties>

<plugin>
  <groupId>com.pelock</groupId>
  <artifactId>jobfuscator-maven-plugin</artifactId>
  <version>${jobfuscator.version}</version>
</plugin>
```

Invoke the goal explicitly:

```bash
mvn com.pelock:jobfuscator-maven-plugin:<version>:obfuscate-sources
```

---

## Usage examples

### Example 1 — minimal Maven project layout

```
my-app/
├── pom.xml
└── src/main/java/com/example/
    ├── Obfuscate.java    ← stub annotation (SOURCE retention), optional
    └── App.java          ← your code with @Obfuscate
```

### Example 2 — minimal `pom.xml` (plugin + compiler)

Replace `<version>` placeholders with your released coordinates:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.example</groupId>
  <artifactId>my-app</artifactId>
  <version>1.0.2-SNAPSHOT</version>

  <properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <maven.compiler.release>11</maven.compiler.release>
    <jobfuscator.apiKey><!-- YOU-API-HERE or leave empty for DEMO VERSION --></jobfuscator.apiKey>    
    <jobfuscator.version><!-- e.g. 1.0.2 --></jobfuscator.version>
  </properties>

  <build>
    <plugins>
      <plugin>
        <groupId>com.pelock</groupId>
        <artifactId>jobfuscator-maven-plugin</artifactId>
        <version>${jobfuscator.version}</version>
        <executions>
          <execution>
            <goals>
              <goal>obfuscate-sources</goal>
            </goals>
            <phase>generate-sources</phase>
          </execution>
        </executions>
        <configuration>
          <apiKey>${jobfuscator.apiKey}</apiKey>
          <enableCompression>true</enableCompression>
          <mixCodeFlow>true</mixCodeFlow>
          <renameVariables>true</renameVariables>
          <renameMethods>true</renameMethods>
          <shuffleMethods>true</shuffleMethods>
          <intsMathCrypt>true</intsMathCrypt>
          <cryptStrings>true</cryptStrings>
          <stringSplit>true</stringSplit>
          <intsToArrays>true</intsToArrays>
          <dblsToArrays>true</dblsToArrays>
          <dblsMathCrypt>true</dblsMathCrypt>
          <stringCharVault>true</stringCharVault>
          <intsFromDoubleMath>true</intsFromDoubleMath>
          <opaqueMixerChain>true</opaqueMixerChain>
          <complexifyBooleans>true</complexifyBooleans>
          <tryFinallyNoise>true</tryFinallyNoise>
          <selfCheck>true</selfCheck>
          <arrayIntCrypt>true</arrayIntCrypt>
          <arrayCharCrypt>true</arrayCharCrypt>
          <arrayDoubleCrypt>true</arrayDoubleCrypt>
          <arrayStringCrypt>true</arrayStringCrypt>
          <selfCheck>true</selfCheck>
        </configuration>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.13.0</version>
        <configuration>
          <release>${maven.compiler.release}</release>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

### Example 3 — `src/main/java/com/example/App.java`

```java
package com.example;

@Obfuscate
public class App {
  public static void main(String[] args) {
    System.out.println("Hello");
  }
}
```

Copy the `@interface Obfuscate` stub from the subsection **Declaring `@Obfuscate` so `javac` accepts it** (later in this README).

### Example 4 — shell: full build with API key

```bash
mvn clean package -Djobfuscator.apiKey="YOUR-ACTIVATION-KEY"
```

### Example 5 — shell: skip obfuscation (compile original sources only)

```bash
mvn clean compile -Djobfuscator.skip=true
```

### Example 6 — shell: only run the plugin goal (no `<execution>` in `pom.xml`)

Replace `1.0.2` with your plugin version:

```bash
mvn com.pelock:jobfuscator-maven-plugin:1.0.2:obfuscate-sources \
  -Djobfuscator.apiKey="YOUR-ACTIVATION-KEY"
```

### Example 7 — shell: tune parallelism

```bash
mvn clean package -Djobfuscator.apiKey="YOUR-KEY" -Djobfuscator.threads=8
```

### Example 8 — Java: call the Web API with `jobfuscator-client`

```java
import com.pelock.jobfuscator.JObfuscatorClient;
import com.pelock.jobfuscator.JObfuscatorResult;

public class ObfuscateFromJava {
  public static void main(String[] args) throws Exception {
    String key = System.getenv("JOBFUSCATOR_API_KEY"); // or pass args[0], can be null for demo limits

    JObfuscatorClient client = new JObfuscatorClient(key);
    // Optional: e.g. client.enableCompression = true;

    String javaSource =
        "@Obfuscate\n"
            + "class Ideone {\n"
            + "  public static void main(String[] a) {\n"
            + "    System.out.println(\"hi\");\n"
            + "  }\n"
            + "}\n";

    JObfuscatorResult result = client.obfuscateJavaSource(javaSource);

    if (result.getError() != JObfuscatorClient.ERROR_SUCCESS) {
      System.err.println("API error code: " + result.getError());
      System.exit(1);
    }

    System.out.println(result.getOutput());

    // Optional license probe:
    // JObfuscatorResult info = client.login();
  }
}
```

Add the `jobfuscator-client` dependency to the same project (coordinates are listed under **Standalone Java client** below).

---

## Usage (Maven)

Bind `obfuscate-sources` to `generate-sources` so obfuscated sources are produced **before** compilation:

```xml
<plugin>
  <groupId>com.pelock</groupId>
  <artifactId>jobfuscator-maven-plugin</artifactId>
  <version>${jobfuscator.version}</version>
  <executions>
    <execution>
      <goals>
        <goal>obfuscate-sources</goal>
      </goals>
      <phase>generate-sources</phase>
    </execution>
  </executions>
  <configuration>
    <apiKey>${jobfuscator.apiKey}</apiKey>
    <enableCompression>true</enableCompression>

    <!-- Global strategies (defaults align with the Web API) -->
    <mixCodeFlow>true</mixCodeFlow>
    <renameVariables>true</renameVariables>
    <renameMethods>true</renameMethods>
    <shuffleMethods>true</shuffleMethods>
    <intsMathCrypt>true</intsMathCrypt>
    <cryptStrings>true</cryptStrings>
    <stringSplit>true</stringSplit>
    <intsToArrays>true</intsToArrays>
    <dblsToArrays>true</dblsToArrays>
    <dblsMathCrypt>true</dblsMathCrypt>
    <stringCharVault>true</stringCharVault>
    <intsFromDoubleMath>true</intsFromDoubleMath>
    <opaqueMixerChain>true</opaqueMixerChain>
    <complexifyBooleans>true</complexifyBooleans>
    <tryFinallyNoise>true</tryFinallyNoise>
    <arrayIntCrypt>true</arrayIntCrypt>
    <arrayCharCrypt>true</arrayCharCrypt>
    <arrayDoubleCrypt>true</arrayDoubleCrypt>
    <arrayStringCrypt>true</arrayStringCrypt>
    <selfCheck>true</selfCheck>
  </configuration>
</plugin>
```

Provide the activation key via `-Djobfuscator.apiKey=…`, a `<properties>` entry, or an encrypted value from [`settings.xml`](https://maven.apache.org/guides/mini/guide-encryption.html). Never commit keys.

### What the plugin does

**Original `.java` files are never overwritten.** For each relative path, the goal reads from `inputDirectory` and writes the API response only under `outputDirectory` (default under `target/generated-sources/…`). If `inputDirectory` and `outputDirectory` resolve to the **same path**, the build fails immediately (otherwise clearing the output tree could delete your sources).

**Scope vs compilation.** The goal walks **every** `*.java` under `inputDirectory`, which defaults to `src/main/java` — the usual main compile source root. If `inputDirectory` is **not** one of `project.getCompileSourceRoots()`, a **warning** is logged so you can align configuration (extra compile roots are still compiled by Maven but would not be obfuscated unless included under `inputDirectory` or via a separate execution).

1. Scans `inputDirectory` (default `src/main/java`) for `*.java` files.
2. Sends each file to the JObfuscator Web API (`multipart/form-data`, optional zlib compression on request and response bodies).
3. Writes transformed sources under `outputDirectory` (default `target/generated-sources/jobfuscator`), preserving package directories.
4. By default (`replaceMainCompileSources` = `true`), removes the compile source root that matches `inputDirectory` from the model and adds `outputDirectory`, so `javac` compiles **obfuscated** sources while **leaving the original tree unchanged on disk**.

`@Obfuscate` markers are interpreted **by the service** from your Java source text; this plugin does not parse annotations locally.

### Behaviour switches

| Property | Meaning |
|----------|---------|
| `skip` / `-Djobfuscator.skip=true` | Skip obfuscation; normal `src/main/java` is compiled. |
| `failOnError` | Fail the build on first API / I/O error (default `true`). |
| `replaceMainCompileSources` | Replace default main sources with generated tree (default `true`). |
| `threads` / `-Djobfuscator.threads` | Concurrent API requests (default **4**). Capped by file count; values `< 1` are treated as `1`. Respect provider rate limits and credits. |

---

## `@Obfuscate` annotations in source code

JObfuscator recognises **`@Obfuscate`** in **Java source** to enable protection:

- **Whole type** — annotate the class, interface, enum, or annotation type.
- **Single member** — annotate individual methods when you do not want whole-class obfuscation.
- **Parameters** — optional attributes use **snake_case** names aligned with the Web API (`rename_methods`, `crypt_strings`, …), so you can tune strategies **per class or per method** on top of global Maven settings.

### Declaring `@Obfuscate` so `javac` accepts it

The compiler needs an **`@Obfuscate`** type. Until you use an official annotation library from PELock (if distributed separately), a **SOURCE**-retention stub is enough for compiling **original** sources:

```java
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Obfuscate {
  boolean mix_code_flow() default true;
  boolean rename_variables() default true;
  boolean rename_methods() default true;
  boolean shuffle_methods() default true;
  boolean ints_math_crypt() default true;
  boolean crypt_strings() default true;
  boolean string_split() default true;
  boolean ints_to_arrays() default true;
  boolean dbls_to_arrays() default true;
  boolean dbls_math_crypt() default true;
  boolean string_char_vault() default true;
  boolean ints_from_double_math() default true;
  boolean opaque_mixer_chain() default true;
  boolean complexify_booleans() default true;
  boolean try_finally_noise() default true;
  boolean array_int_crypt() default true;
  boolean array_char_crypt() default true;
  boolean array_double_crypt() default true;
  boolean array_string_crypt() default true;
  boolean self_check() default true;  
}
```

For marker-only usage you can start from `@interface Obfuscate {}` and narrow `@Target` as needed.

### Example — entire class

```java
import java.io.*;

@Obfuscate
class Ideone {
  public static double calculateSD(double[] numArray) {
    double sum = 0.0;
    double standardDeviation = 0.0;
    int length = numArray.length;

    for (double num : numArray) {
      sum += num;
    }

    double mean = sum / length;

    for (double num : numArray) {
      standardDeviation += Math.pow(num - mean, 2);
    }

    return Math.sqrt(standardDeviation / length);
  }

  public static void main(String[] args) {
    double[] numArray = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    double sd = calculateSD(numArray);
    System.out.format("Standard Deviation = %.6f%n", sd);
  }
}
```

### Example — selected methods only

```java
class Mixed {

  @Obfuscate
  public static double calculateSD(double[] nums) {
    return internalStats(nums);
  }

  public static void main(String[] args) {
    System.out.println(calculateSD(new double[] {1, 2, 3}));
  }

  private static double internalStats(double[] nums) {
    /* ... */
    return 0;
  }
}
```

### Example — interface type and methods

```java
@Obfuscate
public interface PaymentGateway {

  void authorize(String id, double amount);

  @Obfuscate
  default void log(String message) {
    System.err.println(message);
  }
}

public class StripeGateway implements PaymentGateway {

  @Override
  public void authorize(String id, double amount) {
    /* ... */
  }
}
```

### Example — enum

```java
@Obfuscate
public enum Mode {
  FAST,
  SAFE
}
```

### Example — selective strategies

Per-type overrides use the same snake_case attribute names as the Web API:

```java
@Obfuscate(
    rename_methods = false,
    rename_variables = true,
    shuffle_methods = true,
    mix_code_flow = true,
    crypt_strings = true,
    string_split = true,
    ints_math_crypt = true,
    dbls_math_crypt = true,
    string_char_vault = true,
    ints_from_double_math = true,
    opaque_mixer_chain = true,
    complexify_booleans = true,
    try_finally_noise = true,
    ints_to_arrays = true,
    dbls_to_arrays = true,
    array_int_crypt = true,
    array_char_crypt = true,
    array_double_crypt = true,
    array_string_crypt = true,
    self_check = true)
public final class FineGrained {
  /* ... */
}
```

If you **disable strategies globally** in the Maven `<configuration>`, the remote service may ignore `@Obfuscate` markers in source—keep global flags and annotations aligned when tuning builds.

---

## Standalone Java client (`jobfuscator-client`)

```xml
<dependency>
  <groupId>com.pelock</groupId>
  <artifactId>jobfuscator-client</artifactId>
  <version>${jobfuscator.version}</version>
</dependency>
```

Use `com.pelock.jobfuscator.JObfuscatorClient`: `obfuscateJavaSource(String)`, `login()`, and public boolean fields (`enableCompression`, strategy toggles) that mirror the plugin `<configuration>` parameters.

---

## Building this repository

```bash
mvn clean install
```

Optional Invoker IT profile (offline sample with `<skip>true</skip>`):

```bash
mvn clean install -Pinvoker-tests
```

---

## Requirements

- **JDK 11+**
- **Maven 3.9+** (typical)
- **Network access** at build time when obfuscation is not skipped

Bartosz Wójcik

* Visit my site at — https://www.pelock.com
* X — https://x.com/PELock
* GitHub — https://github.com/PELock