package tech.onetap.util.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

public final class LicenseGuard {
    private static final String VALIDATE_URL = "https://clan-exzodus.vercel.app/api/validate";
    private LicenseGuard() {}

    public static void requireValidLicense() {
        try {
            Path keyFile = Path.of(".options", "license-key.txt");
            if (!Files.exists(keyFile)) throw new SecurityException("Запустите Infinyty через официальный лаунчер");
            String key = Files.readString(keyFile).trim();
            if (key.isEmpty()) throw new SecurityException("Лицензионный ключ отсутствует");

            JsonObject request = new JsonObject();
            request.addProperty("key", key);
            request.addProperty("hwid", launcherCompatibleHwid());
            byte[] data = request.toString().getBytes(StandardCharsets.UTF_8);
            HttpURLConnection connection = (HttpURLConnection) new URL(VALIDATE_URL).openConnection();
            connection.setRequestMethod("POST"); connection.setDoOutput(true);
            connection.setConnectTimeout(8000); connection.setReadTimeout(8000);
            connection.setRequestProperty("Content-Type", "application/json");
            try (OutputStream output = connection.getOutputStream()) { output.write(data); }
            int status = connection.getResponseCode();
            String response = new String((status < 400 ? connection.getInputStream() : connection.getErrorStream()).readAllBytes(), StandardCharsets.UTF_8);
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            if (status >= 400 || !json.has("valid") || !json.get("valid").getAsBoolean()) {
                throw new SecurityException(json.has("message") ? json.get("message").getAsString() : "Лицензия недействительна");
            }
        } catch (SecurityException error) {
            throw error;
        } catch (Exception error) {
            throw new SecurityException("Сервер лицензий недоступен. Проверьте интернет и перезапустите лаунчер.", error);
        }
    }

    private static String launcherCompatibleHwid() throws Exception {
        String source = String.join("|",
                env("COMPUTERNAME"), env("PROCESSOR_IDENTIFIER"), env("SystemDrive"),
                "Windows 10", "amd64", env("USERNAME"));
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();
        for (byte value : hash) result.append(String.format("%02x", value));
        return result.toString();
    }

    private static String env(String name) { return System.getenv(name) == null ? "" : System.getenv(name); }
}
