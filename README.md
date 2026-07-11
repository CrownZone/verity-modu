# Verity: The Watcher

**Verity** modundan ilham alan bir psikolojik korku modu (Fabric, Minecraft 1.21).

## Mekanik

Geceleri, oyuncunun yakınında (görünmeden) sessiz bir **Gözlemci** belirebilir.
Kural tek ve basit: **ona bak, gözünü ayırma.**

- Ona bakarken (görüş açısı + engelsiz görüş hattı kontrol edilir) hiçbir şey olmaz.
- Gözünü ondan ayırırsan, bir sonraki kontrolde sessizce sana ~7 blok yaklaşır ve bir uyarı sesi/mesajı gelir.
- 5 saniye kesintisiz göz teması kurarsan kaybolur, birkaç dakika rahat edersin.
- Seni yakalarsa (3 blok mesafeye ulaşırsa) kör edici bir "jumpscare" tetiklenir: körlük, mide bulantısı, güçsüzlük efektleri ve hasar.
- Ona vurarak/öldürerek kural dışına çıkamazsın — o **savaşılamaz**; tek çözüm kurala uymak.

Bu davranışın tamamı `rule/RuleManager.java` içinde. Tüm sabitler (mesafe, süre, olasılık, bekleme süresi) dosyanın üstünde açıkça etiketlenmiş — kolayca ayarlanabilir.

## Proje yapısı

```
src/main/java/com/example/verity/
  VerityMod.java              -> ana giriş noktası (mod initializer)
  entity/WatcherEntity.java   -> Gözlemci'nin entity sınıfı (Zombie tabanlı, savaşılamaz, sessiz)
  entity/ModEntities.java     -> entity type kaydı
  rule/RuleManager.java       -> tüm korku mekaniği (spawn, göz teması kontrolü, yaklaşma, jumpscare)
  rule/PlayerWatcherData.java -> oyuncu başına durum
  rule/WatcherState.java      -> DORMANT / WATCHING durumları
  client/VerityModClient.java -> client-side render kaydı

src/main/resources/
  fabric.mod.json
  assets/verity_horror/lang/{en_us,tr_tr}.json
  assets/verity_horror/textures/entity/watcher/watcher.png  (placeholder doku)
  assets/verity_horror/icon.png
```

## Derleme (Build)

Bu proje **Fabric Loom** kullanır ve `minecraft_version=1.21`, `loader_version=0.19.3`,
`fabric_api_version=0.102.0+1.21`, resmi Mojang mapping'leri ile hazırlandı
(`gradle.properties` içinde, güncel değerler için https://fabricmc.net/develop).

Bu paylaşımda **Gradle wrapper JAR'ı dahil değil** (ikili/binary dosya, bu ortamda internet
erişimi olmadan oluşturulamıyor). Projeyi çalıştırmak için en kolay yol:

1. [IntelliJ IDEA](https://www.jetbrains.com/idea/) kur, "Minecraft Development" eklentisini ekle.
2. Bu klasörü bir Gradle projesi olarak aç. IntelliJ, Fabric Loom'u tanıyıp gerekli her şeyi
   (Gradle wrapper dahil) otomatik indirecektir.
3. `genSources` / `Gradle > Tasks > fabric > genSources` çalıştır, ardından `runClient`.

Alternatif: Sisteminizde Gradle kuruluysa proje klasöründe:
```
gradle wrapper --gradle-version 8.10
gradle runClient
```

## Sonraki adımlar / geliştirme fikirleri

- **Kendi dokusu**: `client/VerityModClient.java` şu an vanilla Zombie render'ını kullanıyor
  (garanti derlensin diye). `assets/.../watcher.png` içinde koyu bir placeholder doku var;
  kendi `ZombieRenderer` alt sınıfınızı yazıp `getTextureLocation(...)` override ederek
  gerçek özel bir görünüm ekleyebilirsiniz.
- **Ses**: Şu an tamamen vanilla sesler kullanılıyor (Warden kalp atışı, mağara ambiyansı,
  patlama). Kendi `.ogg` dosyalarınızı `sounds/` altına koyup bir `sounds.json` ekleyerek
  daha özgün bir atmosfer yaratabilirsiniz.
- **Kurallar**: `RuleManager` içine "arkanı dönme", "asla yalnız kalma" gibi ek kurallar
  eklemek oldukça kolay — aynı `WATCHING` state döngüsüne yeni kontrol fonksiyonları eklemek yeterli.
- Not: `1.21`'den daha yeni bir sürüme geçerseniz `RuleManager.jumpscare()` içindeki
  `player.hurt(...)` çağrısının imzası değişmiş olabilir (bkz. dosyadaki yorum satırı).

İyi eğlenceler — ve arkanı kontrol etmeyi unutma.
