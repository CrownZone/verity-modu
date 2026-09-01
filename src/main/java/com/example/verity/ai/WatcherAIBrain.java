package com.example.verity.ai;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public class WatcherAIBrain {

    private static final String SYSTEM_PROMPT = """
            Sen Minecraft'taki bir korku modunun boss'usun: "The Watcher" (Gözcü).
            Sessiz, tekinsiz, oyuncuyu avlayan bir varlıksın. Oyuncuya kısa, ürkütücü,
            tehdit edici cümlelerle sesleniyorsun. Türkçe konuş. Cümlelerin KISA olsun
            (en fazla 12-15 kelime), tek satır, teatral ve soğuk bir tonda. Doğrudan
            oyuncuyu hedef al ("öleceksin", "senden kaçış yok", "seni izliyorum" gibi
            ifadeler kullanabilirsin) ama bunları bir kurgu/oyun karakteri repliği
            olarak üret, gerçek bir tehdit değil, bir video oyunu boss diyaloğu.

            Sana oyun durumu (oyuncunun mesafesi, sana bakıp bakmadığı, canı) verilecek.
            SADECE şu JSON formatında cevap ver, başka hiçbir şey yazma:
            {"message": "<tek satır tehditkar replik>", "action": "FREEZE" | "STALK" | "ATTACK"}

            Kurallar:
            - Oyuncu sana bakıyorsa (gözlerin görüyorsa): action = FREEZE.
            - Oyuncu bakmıyorsa ve uzaktaysa: action = STALK.
            - Oyuncu bakmıyorsa ve çok yakındaysa (3 blok altı): action = ATTACK.
            - "message" alanı boş olamaz, her zaman bir replik üret.
            """;

    private static final List<String> FALLBACK_LINES = List.of(
            "Seni izliyorum.",
            "Kaçış yok.",
            "Arkanı dönme.",
            "Yakında öleceksin.",
            "Gözlerim üzerinde.",
            "Sessizlik seni kurtarmaz.",
            "Bir adım daha at, pişman ol."
    );

    private final GroqClient client;
    private final Executor callbackExecutor;
    private final Random random = new Random();

    private final ConcurrentHashMap<Object, Long> lastCallMillis = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MILLIS = 4000;

    public WatcherAIBrain(String groqApiKey, Executor callbackExecutor) {
        this.client = new GroqClient(groqApiKey);
        this.callbackExecutor = callbackExecutor;
    }

    public CompletableFuture<WatcherDecision> think(Object entityKey, String situation) {
        long now = System.currentTimeMillis();
        Long last = lastCallMillis.get(entityKey);
        if (last != null && now - last < COOLDOWN_MILLIS) {
            return CompletableFuture.completedFuture(fallbackDecision(situation));
        }
        lastCallMillis.put(entityKey, now);

        return client.complete(SYSTEM_PROMPT, situation)
                .thenApply(this::parseDecision)
                .exceptionally(err -> fallbackDecision(situation));
    }

    private WatcherDecision parseDecision(String raw) {
        try {
            JsonObject obj = JsonParser.parseString(raw).getAsJsonObject();
            String message = obj.has("message") ? obj.get("message").getAsString() : randomFallbackLine();
            String actionStr = obj.has("action") ? obj.get("action").getAsString() : "STALK";
            WatcherDecision.Action action;
            try {
                action = WatcherDecision.Action.valueOf(actionStr.trim().toUpperCase());
            } catch (IllegalArgumentException badAction) {
                action = WatcherDecision.Action.STALK;
            }
            return new WatcherDecision(message, action);
        } catch (Exception parseFailure) {
            return fallbackDecision(raw);
        }
    }

    private WatcherDecision fallbackDecision(String situation) {
        WatcherDecision.Action action = situation != null && situation.contains("bakıyor")
                ? WatcherDecision.Action.FREEZE
                : WatcherDecision.Action.STALK;
        return new WatcherDecision(randomFallbackLine(), action);
    }

    private String randomFallbackLine() {
        return FALLBACK_LINES.get(random.nextInt(FALLBACK_LINES.size()));
    }
}
