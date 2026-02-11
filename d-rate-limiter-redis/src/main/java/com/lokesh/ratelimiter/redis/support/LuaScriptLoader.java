package com.lokesh.ratelimiter.redis.support;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class to load Lua scripts from the classpath and cache their content and SHA1 hashes.
 * This is used to optimize Redis execution by using EVALSHA instead of sending the full script every time.
 */
public class LuaScriptLoader {

    private final Map<String, ScriptInfo> cache = new ConcurrentHashMap<>();

    /**
     * Internal data structure to hold cached script information.
     */
    private record ScriptInfo(String content, String sha1) {}

    /**
     * Retrieves the content of the Lua script at the given classpath path.
     * Uses an internal thread-safe cache for subsequent requests.
     *
     * @param path The classpath-relative path to the .lua file.
     * @return The raw string content of the script.
     * @throws RuntimeException if the script cannot be found or read.
     */
    public String getScript(String path) {
        return getOrLoad(path).content();
    }

    /**
     * Retrieves the SHA1 hash of the Lua script at the given classpath path.
     * Uses an internal thread-safe cache for subsequent requests.
     *
     * @param path The classpath-relative path to the .lua file.
     * @return The 40-character hexadecimal SHA1 hash of the script.
     * @throws RuntimeException if the script cannot be found or read.
     */
    public String getSha(String path) {
        return getOrLoad(path).sha1();
    }

    /**
     * Atomically retrieves or loads the script info from the classpath.
     */
    private ScriptInfo getOrLoad(String path) {
        return cache.computeIfAbsent(path, this::loadFromClasspath);
    }

    /**
     * Performs the actual classpath resource loading and SHA1 calculation.
     */
    private ScriptInfo loadFromClasspath(String path) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new RuntimeException("Lua script not found on classpath: " + path);
            }
            // Modern Java (9+) approach: direct read into string is faster than Scanner
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            String sha1 = calculateSha1(content);
            return new ScriptInfo(content, sha1);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read Lua script from classpath: " + path, e);
        }
    }

    /**
     * Calculates the SHA1 hash of the given string input.
     * Standard implementation for Redis EVALSHA compatibility.
     */
    private String calculateSha1(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 algorithm not found", e);
        }
    }
}