package com.pelock.jobfuscator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;

/**
 * Java client mirroring the {@code jobfuscator} npm SDK multipart/zlib contract against the
 * JObfuscator Web API.
 */
public class JObfuscatorClient {

  public static final String DEFAULT_API_URL = "https://www.pelock.com/api/jobfuscator/v1";

  public static final int ERROR_SUCCESS = 0;
  public static final int ERROR_INPUT_SIZE = 1;
  public static final int ERROR_INPUT = 2;
  public static final int ERROR_PARSING = 3;
  public static final int ERROR_OBFUSCATION = 4;
  public static final int ERROR_OUTPUT = 5;

  private static final ObjectMapper JSON = new ObjectMapper();

  private final String apiKey;

  private String apiUrl = DEFAULT_API_URL;

  /** Mirrors JS {@code enableCompression}. */
  public boolean enableCompression = true;

  public boolean mixCodeFlow = true;
  public boolean renameVariables = true;
  public boolean renameMethods = true;
  public boolean shuffleMethods = true;
  public boolean intsMathCrypt = true;
  public boolean cryptStrings = true;
  public boolean stringSplit = true;
  public boolean intsToArrays = true;
  public boolean dblsToArrays = true;
  public boolean dblsMathCrypt = true;
  public boolean stringCharVault = true;
  public boolean intsFromDoubleMath = true;
  public boolean opaqueMixerChain = true;
  public boolean complexifyBooleans = true;
  public boolean tryFinallyNoise = true;
  public boolean selfCheck = true;
  public boolean arrayIntCrypt = true;
  public boolean arrayCharCrypt = true;
  public boolean arrayDoubleCrypt = true;
  public boolean arrayStringCrypt = true;

  public JObfuscatorClient(String apiKey) {
    this.apiKey = apiKey;
  }

  /** Override API endpoint (defaults to {@link #DEFAULT_API_URL}). Intended for tests. */
  public void setApiUrl(String apiUrl) {
    this.apiUrl = Objects.requireNonNull(apiUrl, "apiUrl");
  }

  public String getApiUrl() {
    return apiUrl;
  }

  public JObfuscatorResult login() throws IOException {
    Map<String, String> params = new LinkedHashMap<>();
    params.put("command", "login");
    return postRequest(params, false);
  }

  public JObfuscatorResult obfuscateJavaSource(String javaSource) throws IOException {
    if (javaSource == null || javaSource.isEmpty()) {
      return new JObfuscatorResult(ERROR_INPUT, null, null, null, null, null, null);
    }
    Map<String, String> params = new LinkedHashMap<>();
    params.put("command", "obfuscate");
    params.put("source", javaSource);
    return postRequest(params, true);
  }

  private JObfuscatorResult postRequest(Map<String, String> params, boolean mayCompressSource)
      throws IOException {
    LinkedHashMap<String, String> merged = new LinkedHashMap<>(params);

    if (apiKey != null && !apiKey.isEmpty()) {
      merged.put("key", apiKey);
    }

    if (mixCodeFlow) merged.put("mix_code_flow", "1");
    if (renameVariables) merged.put("rename_variables", "1");
    if (renameMethods) merged.put("rename_methods", "1");
    if (shuffleMethods) merged.put("shuffle_methods", "1");
    if (intsMathCrypt) merged.put("ints_math_crypt", "1");
    if (cryptStrings) merged.put("crypt_strings", "1");
    if (stringSplit) merged.put("string_split", "1");
    if (intsToArrays) merged.put("ints_to_arrays", "1");
    if (dblsToArrays) merged.put("dbls_to_arrays", "1");
    merged.put("remove_comments", "1");
    if (dblsMathCrypt) merged.put("dbls_math_crypt", "1");
    if (stringCharVault) merged.put("string_char_vault", "1");
    if (intsFromDoubleMath) merged.put("ints_from_double_math", "1");
    if (opaqueMixerChain) merged.put("opaque_mixer_chain", "1");
    if (complexifyBooleans) merged.put("complexify_booleans", "1");
    if (tryFinallyNoise) merged.put("try_finally_noise", "1");
    if (selfCheck) merged.put("self_check", "1");
    if (arrayIntCrypt) merged.put("array_int_crypt", "1");
    if (arrayCharCrypt) merged.put("array_char_crypt", "1");
    if (arrayDoubleCrypt) merged.put("array_double_crypt", "1");
    if (arrayStringCrypt) merged.put("array_string_crypt", "1");

    boolean compressedPayload =
        enableCompression && mayCompressSource && merged.containsKey("source");

    if (compressedPayload) {
      String plain = merged.remove("source");
      byte[] zipped = zlibCompress(plain.getBytes(StandardCharsets.UTF_8));
      merged.put("source", Base64.getEncoder().encodeToString(zipped));
      merged.put("compression", "1");
    }

    MultipartEntityBuilder builder = MultipartEntityBuilder.create();
    for (Map.Entry<String, String> e : merged.entrySet()) {
      builder.addTextBody(e.getKey(), e.getValue(), ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8));
    }
    HttpEntity entity = builder.build();

    HttpPost post = new HttpPost(apiUrl);
    post.setEntity(entity);
    post.addHeader("User-Agent", "PELock JObfuscator");

    try (CloseableHttpClient http = HttpClients.createDefault();
        CloseableHttpResponse response = http.execute(post)) {
      String body;
      try {
        body =
            response.getEntity() != null
                ? EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8)
                : "";
      } catch (ParseException e) {
        throw new IOException("Could not read HTTP entity", e);
      }
      return parseJsonResponse(body, compressedPayload);
    }
  }

  static byte[] zlibCompress(byte[] input) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (DeflaterOutputStream dos =
        new DeflaterOutputStream(out, new Deflater(9, false))) {
      dos.write(input);
    }
    return out.toByteArray();
  }

  static String zlibDecompressBase64(String base64) throws IOException {
    byte[] decoded = Base64.getDecoder().decode(base64);
    try (InflaterInputStream iis =
            new InflaterInputStream(new ByteArrayInputStream(decoded), new Inflater(false));
        ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      byte[] buf = new byte[4096];
      int n;
      while ((n = iis.read(buf)) != -1) {
        bos.write(buf, 0, n);
      }
      return new String(bos.toByteArray(), StandardCharsets.UTF_8);
    }
  }

  private static JObfuscatorResult parseJsonResponse(
      String responseText, boolean depackOutputIfCompressed)
      throws IOException {
    if (responseText == null || responseText.isEmpty()) {
      throw new IOException("empty API response");
    }

    JsonNode root = JSON.readTree(responseText);
    if (root == null || !root.isObject()) {
      throw new IOException("invalid API JSON");
    }

    int error = root.path("error").asInt(ERROR_INPUT);
    String output = textOrNull(root, "output");
    if (depackOutputIfCompressed
        && error == ERROR_SUCCESS
        && output != null
        && !output.isEmpty()) {
      output = zlibDecompressBase64(output);
    }

    return new JObfuscatorResult(
        error,
        output,
        root.hasNonNull("demo") ? root.get("demo").asBoolean() : null,
        root.hasNonNull("credits_left") ? root.get("credits_left").asInt() : null,
        root.hasNonNull("credits_total") ? root.get("credits_total").asInt() : null,
        root.hasNonNull("expired") ? root.get("expired").asBoolean() : null,
        root.hasNonNull("string_limit") ? root.get("string_limit").asInt() : null);
  }

  private static String textOrNull(JsonNode root, String field) {
    JsonNode n = root.get(field);
    if (n == null || n.isNull()) return null;
    return n.asText();
  }
}
