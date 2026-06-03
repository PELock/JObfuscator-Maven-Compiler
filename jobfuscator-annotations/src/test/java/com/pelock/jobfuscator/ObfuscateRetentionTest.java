package com.pelock.jobfuscator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.junit.jupiter.api.Test;

class ObfuscateRetentionTest {

  @Test
  void obfuscateHasSourceRetention() {
    Retention retention = Obfuscate.class.getAnnotation(Retention.class);
    assertEquals(RetentionPolicy.SOURCE, retention.value());
  }
}
