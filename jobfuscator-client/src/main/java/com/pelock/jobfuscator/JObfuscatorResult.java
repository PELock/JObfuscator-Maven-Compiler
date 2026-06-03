package com.pelock.jobfuscator;

import java.util.Objects;

/** Parsed JSON response from the JObfuscator Web API. */
public final class JObfuscatorResult {

  private final int error;
  private final String output;
  private final Boolean demo;
  private final Integer creditsLeft;
  private final Integer creditsTotal;
  private final Boolean expired;
  private final Integer stringLimit;

  public JObfuscatorResult(
      int error,
      String output,
      Boolean demo,
      Integer creditsLeft,
      Integer creditsTotal,
      Boolean expired,
      Integer stringLimit) {
    this.error = error;
    this.output = output;
    this.demo = demo;
    this.creditsLeft = creditsLeft;
    this.creditsTotal = creditsTotal;
    this.expired = expired;
    this.stringLimit = stringLimit;
  }

  public int getError() {
    return error;
  }

  public String getOutput() {
    return output;
  }

  public Boolean getDemo() {
    return demo;
  }

  public Integer getCreditsLeft() {
    return creditsLeft;
  }

  public Integer getCreditsTotal() {
    return creditsTotal;
  }

  public Boolean getExpired() {
    return expired;
  }

  public Integer getStringLimit() {
    return stringLimit;
  }

  public boolean isSuccess() {
    return error == JObfuscatorClient.ERROR_SUCCESS;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    JObfuscatorResult that = (JObfuscatorResult) o;
    return error == that.error
        && Objects.equals(output, that.output)
        && Objects.equals(demo, that.demo)
        && Objects.equals(creditsLeft, that.creditsLeft)
        && Objects.equals(creditsTotal, that.creditsTotal)
        && Objects.equals(expired, that.expired)
        && Objects.equals(stringLimit, that.stringLimit);
  }

  @Override
  public int hashCode() {
    return Objects.hash(error, output, demo, creditsLeft, creditsTotal, expired, stringLimit);
  }
}
