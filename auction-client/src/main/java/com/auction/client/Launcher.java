package com.auction.client;

/**
 * Entry point khi build fat JAR (java -jar auction-client.jar).
 * KHÔNG extends Application để tránh lỗi "JavaFX runtime components are missing"
 * khi JVM cố load class extends Application trực tiếp ngoài module-path.
 * Khi mvn javafx:run dùng trực tiếp ClientApp.main() — không qua Launcher.
 */
public final class Launcher {

    private Launcher() {
        // Utility entry point, không cho instantiate.
    }

    public static void main(String[] args) {
        ClientApp.main(args);
    }
}
