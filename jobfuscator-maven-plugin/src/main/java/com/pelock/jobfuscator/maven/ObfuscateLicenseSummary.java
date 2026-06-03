package com.pelock.jobfuscator.maven;

import com.pelock.jobfuscator.JObfuscatorResult;
import java.util.List;

/** Builds license/credit suffixes for obfuscate-sources completion logs. */
final class ObfuscateLicenseSummary {

  private boolean demo;
  private boolean expired;
  private Integer creditsLeft;
  private Integer creditsTotal;

  static ObfuscateLicenseSummary fromResults(List<JObfuscatorResult> results) {
    ObfuscateLicenseSummary summary = new ObfuscateLicenseSummary();
    if (results == null) {
      return summary;
    }
    for (JObfuscatorResult result : results) {
      if (result == null) {
        continue;
      }
      summary.merge(result);
    }
    return summary;
  }

  private void merge(JObfuscatorResult result) {
    if (Boolean.TRUE.equals(result.getDemo())) {
      demo = true;
    }
    if (Boolean.TRUE.equals(result.getExpired())) {
      expired = true;
    }
    if (!Boolean.TRUE.equals(result.getDemo())) {
      if (result.getCreditsLeft() != null) {
        creditsLeft = result.getCreditsLeft();
      }
      if (result.getCreditsTotal() != null) {
        creditsTotal = result.getCreditsTotal();
      }
    }
  }

  static String formatProcessedMessage(int fileCount, ObfuscateLicenseSummary summary) {
    StringBuilder message =
        new StringBuilder("JObfuscator processed ")
            .append(fileCount)
            .append(fileCount == 1 ? " Java source file" : " Java source files")
            .append('.');
    if (summary == null) {
      return message.toString();
    }
    if (summary.demo) {
      message.append(" (demo version)");
    } else if (summary.creditsLeft != null && summary.creditsTotal != null) {
      message
          .append(' ')
          .append(summary.creditsLeft)
          .append(" credits left out of ")
          .append(summary.creditsTotal)
          .append('.');
    } else if (summary.creditsLeft != null) {
      message.append(' ').append(summary.creditsLeft).append(" credits left.");
    }
    if (summary.expired) {
      message.append(" (your key has expired)");
    }
    return message.toString();
  }

  private ObfuscateLicenseSummary() {}
}
