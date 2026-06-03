package com.pelock.jobfuscator;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JObfuscatorClientTest {

  private MockWebServer server;

  @BeforeEach
  void setUp() throws IOException {
    server = new MockWebServer();
    server.start();
  }

  @AfterEach
  void tearDown() throws IOException {
    server.shutdown();
  }

  @Test
  void obfuscateMultipartIncludesStrategyFieldsAndRemoveComments() throws Exception {
    server.enqueue(new MockResponse().setBody("{\"error\":0,\"output\":\"out\"}"));

    JObfuscatorClient client = new JObfuscatorClient(null);
    client.setApiUrl(server.url("/").toString());
    client.enableCompression = false;

    JObfuscatorResult res = client.obfuscateJavaSource("class A {}");
    assertEquals(JObfuscatorClient.ERROR_SUCCESS, res.getError());
    assertEquals("out", res.getOutput());

    RecordedRequest req = server.takeRequest();
    assertEquals("PELock JObfuscator", req.getHeader("User-Agent"));
    String body = req.getBody().readUtf8();
    assertTrue(body.contains("name=\"command\""));
    assertTrue(body.contains("obfuscate"));
    assertTrue(body.contains("name=\"mix_code_flow\""));
    assertTrue(body.contains("name=\"self_check\""));
    assertTrue(body.contains("name=\"remove_comments\""));
    assertFalse(body.contains("name=\"key\""));
  }

  @Test
  void omitsSelfCheckWhenDisabled() throws Exception {
    server.enqueue(new MockResponse().setBody("{\"error\":0,\"output\":\"out\"}"));

    JObfuscatorClient client = new JObfuscatorClient(null);
    client.setApiUrl(server.url("/").toString());
    client.enableCompression = false;
    client.selfCheck = false;

    client.obfuscateJavaSource("class A {}");
    String body = server.takeRequest().getBody().readUtf8();
    assertFalse(body.contains("name=\"self_check\""));
  }

  @Test
  void sendsApiKeyWhenProvided() throws Exception {
    server.enqueue(new MockResponse().setBody("{\"error\":0,\"output\":\"x\"}"));

    JObfuscatorClient client = new JObfuscatorClient("secret");
    client.setApiUrl(server.url("/").toString());
    client.enableCompression = false;

    client.obfuscateJavaSource("class B {}");
    String body = server.takeRequest().getBody().readUtf8();
    assertTrue(body.contains("name=\"key\""));
    assertTrue(body.contains("secret"));
  }

  @Test
  void decompressesCompressedSuccessOutput() throws Exception {
    String plain = "public class X {}";
    String b64 =
        Base64.getEncoder()
            .encodeToString(JObfuscatorClient.zlibCompress(plain.getBytes(StandardCharsets.UTF_8)));
    server.enqueue(new MockResponse().setBody("{\"error\":0,\"output\":\"" + b64 + "\"}"));

    JObfuscatorClient client = new JObfuscatorClient(null);
    client.setApiUrl(server.url("/").toString());
    client.enableCompression = true;

    JObfuscatorResult res = client.obfuscateJavaSource("class Y {}");
    assertEquals(JObfuscatorClient.ERROR_SUCCESS, res.getError());
    assertEquals(plain, res.getOutput());

    RecordedRequest req = server.takeRequest();
    String sent = req.getBody().readUtf8();
    assertTrue(sent.contains("name=\"compression\""));
  }

  @Test
  void parsesLicenseFieldsFromResponse() throws Exception {
    server.enqueue(
        new MockResponse()
            .setBody(
                "{\"error\":0,\"output\":\"ok\",\"demo\":false,\"credits_left\":12,"
                    + "\"credits_total\":50,\"expired\":false}"));

    JObfuscatorClient client = new JObfuscatorClient("key");
    client.setApiUrl(server.url("/").toString());
    client.enableCompression = false;

    JObfuscatorResult res = client.obfuscateJavaSource("class A {}");
    assertEquals(JObfuscatorClient.ERROR_SUCCESS, res.getError());
    assertEquals(false, res.getDemo());
    assertEquals(12, res.getCreditsLeft());
    assertEquals(50, res.getCreditsTotal());
    assertEquals(false, res.getExpired());
  }

  @Test
  void emptySourceReturnsInputErrorWithoutNetwork() throws IOException {
    JObfuscatorClient client = new JObfuscatorClient(null);
    client.setApiUrl(server.url("/").toString());

    JObfuscatorResult res = client.obfuscateJavaSource("");
    assertEquals(JObfuscatorClient.ERROR_INPUT, res.getError());
    assertEquals(0, server.getRequestCount());
  }

  @Test
  void zlibRoundTrip() throws IOException {
    byte[] data = "demo payload".getBytes(StandardCharsets.UTF_8);
    byte[] zipped = JObfuscatorClient.zlibCompress(data);
    String back =
        JObfuscatorClient.zlibDecompressBase64(Base64.getEncoder().encodeToString(zipped));
    assertEquals("demo payload", back);
  }
}
