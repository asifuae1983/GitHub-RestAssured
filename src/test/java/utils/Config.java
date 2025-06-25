package utils;

public class Config {
    public static String getBaseUri() {
        String env = System.getenv("GITHUB_API_BASE_URI");
        return env != null ? env : "https://api.github.com";
    }

    public static String getAuthToken() {
        return System.getenv("GITHUB_TOKEN");
    }
}