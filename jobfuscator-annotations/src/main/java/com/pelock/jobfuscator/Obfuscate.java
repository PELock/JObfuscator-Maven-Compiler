package com.pelock.jobfuscator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a type or method for JObfuscator protection in Java source.
 *
 * <p>Attributes use snake_case names aligned with the JObfuscator Web API. The remote service
 * reads {@code @Obfuscate} from source text; this annotation is compile-time only ({@link
 * RetentionPolicy#SOURCE}).
 *
 * @see <a href="https://www.pelock.com/products/jobfuscator">JObfuscator product</a>
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Obfuscate {

  /** Enable non-linear control flow mixing ({@code mix_code_flow}). */
  boolean mix_code_flow() default true;

  /** Rename local variables ({@code rename_variables}). */
  boolean rename_variables() default true;

  /** Rename methods ({@code rename_methods}). */
  boolean rename_methods() default true;

  /** Shuffle method order ({@code shuffle_methods}). */
  boolean shuffle_methods() default true;

  /** Encrypt integer literals via math ({@code ints_math_crypt}). */
  boolean ints_math_crypt() default true;

  /** Encrypt string literals ({@code crypt_strings}). */
  boolean crypt_strings() default true;

  /** Split string literals ({@code string_split}). */
  boolean string_split() default true;

  /** Represent ints as arrays ({@code ints_to_arrays}). */
  boolean ints_to_arrays() default true;

  /** Represent doubles as arrays ({@code dbls_to_arrays}). */
  boolean dbls_to_arrays() default true;

  /** Encrypt double literals via math ({@code dbls_math_crypt}). */
  boolean dbls_math_crypt() default true;

  /** String character vault ({@code string_char_vault}). */
  boolean string_char_vault() default true;

  /** Derive ints from double math ({@code ints_from_double_math}). */
  boolean ints_from_double_math() default true;

  /** Opaque mixer chain ({@code opaque_mixer_chain}). */
  boolean opaque_mixer_chain() default true;

  /** Complexify boolean expressions ({@code complexify_booleans}). */
  boolean complexify_booleans() default true;

  /** Add try/finally noise ({@code try_finally_noise}). */
  boolean try_finally_noise() default true;

  /** Encrypt int arrays ({@code array_int_crypt}). */
  boolean array_int_crypt() default true;

  /** Encrypt char arrays ({@code array_char_crypt}). */
  boolean array_char_crypt() default true;

  /** Encrypt double arrays ({@code array_double_crypt}). */
  boolean array_double_crypt() default true;

  /** Encrypt string arrays ({@code array_string_crypt}). */
  boolean array_string_crypt() default true;

  /** Enable self-check logic ({@code self_check}). */
  boolean self_check() default true;
}
