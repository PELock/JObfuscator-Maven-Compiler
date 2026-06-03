package com.pelock.jobfuscator.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.pelock.jobfuscator.JObfuscatorClient;
import com.pelock.jobfuscator.JObfuscatorResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class ObfuscateLicenseSummaryTest {

  @Test
  void licensedCreditsWhenNotDemo() {
    JObfuscatorResult r =
        new JObfuscatorResult(
            JObfuscatorClient.ERROR_SUCCESS, "out", false, 7, 100, false, null);
    String message =
        ObfuscateLicenseSummary.formatProcessedMessage(
            2, ObfuscateLicenseSummary.fromResults(List.of(r, r)));
    assertEquals(
        "JObfuscator processed 2 Java source files. 7 credits left out of 100.", message);
  }

  @Test
  void demoVersionSuffix() {
    JObfuscatorResult r =
        new JObfuscatorResult(
            JObfuscatorClient.ERROR_SUCCESS, "out", true, 0, 10, false, null);
    String message =
        ObfuscateLicenseSummary.formatProcessedMessage(
            1, ObfuscateLicenseSummary.fromResults(List.of(r)));
    assertEquals("JObfuscator processed 1 Java source file. (demo version)", message);
  }

  @Test
  void expiredSuffixWithCredits() {
    JObfuscatorResult r =
        new JObfuscatorResult(
            JObfuscatorClient.ERROR_SUCCESS, "out", false, 0, 50, true, null);
    String message =
        ObfuscateLicenseSummary.formatProcessedMessage(
            3, ObfuscateLicenseSummary.fromResults(List.of(r)));
    assertEquals(
        "JObfuscator processed 3 Java source files. 0 credits left out of 50. (your key has expired)",
        message);
  }

  @Test
  void demoTakesPrecedenceOverCredits() {
    JObfuscatorResult licensed =
        new JObfuscatorResult(
            JObfuscatorClient.ERROR_SUCCESS, "a", false, 99, 100, false, null);
    JObfuscatorResult demo =
        new JObfuscatorResult(
            JObfuscatorClient.ERROR_SUCCESS, "b", true, 1, 10, false, null);
    String message =
        ObfuscateLicenseSummary.formatProcessedMessage(
            2, ObfuscateLicenseSummary.fromResults(List.of(licensed, demo)));
    assertEquals("JObfuscator processed 2 Java source files. (demo version)", message);
  }

  @Test
  void demoAndExpired() {
    JObfuscatorResult r =
        new JObfuscatorResult(
            JObfuscatorClient.ERROR_SUCCESS, "out", true, null, null, true, null);
    String message =
        ObfuscateLicenseSummary.formatProcessedMessage(
            1, ObfuscateLicenseSummary.fromResults(List.of(r)));
    assertEquals(
        "JObfuscator processed 1 Java source file. (demo version) (your key has expired)",
        message);
  }
}
