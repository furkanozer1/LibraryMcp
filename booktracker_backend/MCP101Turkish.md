
# MCP Sunucusu: Kapsamlı Rehber

## İçindekiler
1.  MCP Sunucusu Nedir?
2.  Neden Bir MCP Sunucusu Kullanmalı?
3.  Proje Yapısına Derinlemesine Bakış
4.  Detaylı Kod İncelemesi
5.  Kendi MCP Sunucunuzu Nasıl Uygularsınız?
6.  Test Etme ve Dağıtım
7.  En İyi Uygulamalar
8.  Sık Karşılaşılan Sorunlar ve Çözümleri
9.  Mevcut Bir Sisteme Swagger ile MCP Uygulamak

---

## 1. MCP Sunucusu Nedir?

MCP (Model Context Protocol) Sunucusu, yapay zeka (AI) modellerinin uygulamanızın verileri ve işlevselliği ile etkileşime girmesi için standartlaştırılmış bir yoldur. Yapay zeka sistemlerinin, uygulamanızın yeteneklerini anlamasını ve kullanmasını sağlayan bir tercüman gibidir.

**Temel özellikleri:**
- JSON-RPC 2.0 protokolünü kullanır
- İşlevselliği dışa açmak için standartlaştırılmış bir yol sağlar
- AI modellerinin uygulamanızın yeteneklerini keşfetmesini ve kullanmasını sağlar
- HTTP/HTTPS üzerinden çalışır

## 2. Neden Bir MCP Sunucusu Kullanmalı?

1.  **Yapay Zeka Entegrasyonu**: AI modellerinin uygulamanızla etkileşime girmesine olanak tanır.
2.  **Standardizasyon**: İyi tanımlanmış protokoller (JSON-RPC 2.0) kullanır.
3.  **Güvenlik**: Uygulamanızın işlevselliğine kontrollü erişim sağlar.
4.  **Genişletilebilirlik**: Yeni araçlar ve yetenekler eklemek kolaydır.
5.  **Sorumlulukların Ayrılması**: Yapay zeka etkileşim mantığını temel iş mantığından ayrı tutar.

## 3. Proje Yapısına Derinlemesine Bakış

Kitap Takip (Book Tracker) MCP Sunucumuzun her bir bileşenini inceleyelim:

### 3.1 Veri Katmanı (Data Layer)
- `Book.java`: Veri modelimizi tanımlar.
- `BookRepository.java`: Veritabanı işlemlerini yürütür.

### 3.2 Servis Katmanı (Service Layer)
- `BookTools.java`: İş mantığını içerir.

### 3.3 API Katmanı (API Layer)
- `McpServerController.java`: MCP protokolünü yönetir.

### 3.4 Konfigürasyon (Configuration)
- `application.properties`: Uygulama yapılandırması.
- `pom.xml`: Maven bağımlılıkları.

## 4. Detaylı Kod İncelemesi

### 4.1 Book Entity Sınıfı (Book.java)
Bu, kitap verilerimizin planıdır. Sistemimizdeki her kitabın doldurması gereken bir form gibi düşünebilirsiniz:

```java
// Spring'e bunun bir veritabanı tablosu olduğunu söyler
@Entity
public class Book {
    // Her kitap için benzersiz ID (seri numarası gibi)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Kitabın başlığı (ör. "Muhteşem Gatsby")
    private String title;
    
    // Kitabın yazarı (ör. "F. Scott Fitzgerald")
    private String author;
    
    // ISBN numarası (benzersiz kitap tanımlayıcısı, barkod gibi)
    private String isbn;
    
    // Constructor'lar (yeni Book nesneleri oluşturmak için özel metotlar)
    // Getter ve setter'lar (kitabın bilgilerini okumak/güncellemek için metotlar)
}
```

**Önemli Noktalar:**
- `@Entity`: Bu sınıfı bir veritabanı tablosu olarak işaretler.
- `@Id`: Bu alanı benzersiz tanımlayıcı olarak işaretler.
- `@GeneratedValue`: ID'nin otomatik artan olmasını sağlar.
- `private` alanlar: Gerçek kitap verilerini saklar.

### 4.2 Repository Sınıfı (BookRepository.java)
Bu, veritabanındaki kitapları nasıl bulacağını ve organize edeceğini bilen bir kütüphaneci gibidir. Siz sadece neye ihtiyacınız olduğunu söylersiniz, o gerisini halleder:

```java
// Spring'e bunun bir repository (veri erişim katmanı) olduğunu söyler
@Repository
// Ücretsiz CRUD işlemleri almak için JpaRepository'den kalıtım alır
public interface BookRepository extends JpaRepository<Book, Long> {
    // Kitapları başlıklarına göre bulur (ör. "1984" adlı tüm kitaplar)
    List<Book> findByTitle(String title);
    
    // Kitapları yazarlarına göre bulur (ör. J.K. Rowling'in tüm kitapları)
    List<Book> findByAuthor(String author);
}
```

**Burada ne oluyor?**
- `extends JpaRepository<Book, Long>`: Bize `save()`, `findById()`, `findAll()`, `delete()` gibi metotları ücretsiz olarak sağlar.
- `findByTitle` gibi metot isimleri Spring tarafından otomatik olarak uygulanır!
- Gerçek veritabanı sorgularını yazmanıza gerek yok - Spring Data JPA bunu sizin için yapar.

### 4.3 Servis Katmanı (BookTools.java)
Asıl işin yapıldığı yer burasıdır. Kütüphanenin kitapla ilgili tüm görevleri nasıl yapacağını bilen personeli gibi düşünebilirsiniz:

```java
// Spring'e bunun bir servis bileşeni olduğunu söyler
@Service
public class BookTools {
    // Veritabanına erişmek için kullanacağımız repository (kütüphanecimiz)
    private final BookRepository repo;
    
    // Constructor injection - Spring, BookRepository'yi otomatik olarak sağlar
    public BookTools(BookRepository repo) {
        this.repo = repo;
    }
    
    // Veritabanındaki tüm kitapları alır
    public Flux<Book> listAllBooks() {
        return Mono.fromCallable(repo::findAll)  // 1. Tüm kitapları al (bloke eden operasyon)
                .flatMapMany(Flux::fromIterable)  // 2. List<Book>'u Flux<Book>'a dönüştür
                .subscribeOn(Schedulers.boundedElastic());  // 3. Ayrı bir thread'de çalıştır
    }
    
    // Diğer CRUD operasyonları için metotlar (Create, Read, Update, Delete)
    // Her metot benzer bir desen izler:
    // 1. Bazı parametreler al (kitap ID'si veya kitap detayları gibi)
    // 2. Operasyonu gerçekleştirmek için repository'yi kullan
    // 3. Sonucu reaktif bir şekilde döndür (Mono veya Flux)
}
```

**Önemli Noktalar:**
- `@Service`: Bu sınıfı bir servis bileşeni olarak işaretler.
- Constructor injection: Bağımlılıkları almanın önerilen yolu.
- Reaktif programlama: Bloke etmeyen operasyonlar için `Flux` (0..N öğe) ve `Mono` (0..1 öğe) kullanır.

### 4.4 REST Controller (BookStreamController.java)
Bu, kütüphanenin danışma masası gibidir - gelen tüm web isteklerini yönetir ve yanıtları gönderir:

```java
// "/api/books" ile başlayan istekleri yönetir
@RestController
@RequestMapping("/api/books")
public class BookStreamController {
    private final BookTools tools;
    
    // Birden fazla aboneye güncelleme gönderebilen özel bir sink
    private final Sinks.Many<Book> sink = 
        Sinks.many().multicast().onBackpressureBuffer(Queues.SMALL_BUFFER_SIZE, false);

    // Constructor injection
    public BookStreamController(BookTools tools) {
        this.tools = tools;
    }
    
    // Gerçek zamanlı kitap güncellemeleri (canlı yayın gibi)
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Book> stream() {
        // Gerçek güncellemeleri, bağlantıyı canlı tutmak için periyodik pinglerle birleştir
        return Flux.merge(
            sink.asFlux(),  // Gerçek kitap güncellemeleri
            Flux.interval(Duration.ofSeconds(15))
                .map(i -> new Book("ping", "", ""))  // Heartbeat (yaşam sinyali)
        );
    }
    
    // Tüm kitapları al
    @GetMapping
    public Flux<Book> all() { 
        return tools.listAllBooks(); 
    }
    
    // ID'ye göre tek bir kitap al
    @GetMapping("/{id}")
    public Flux<Book> byId(@PathVariable Long id) { 
        return tools.getBookById(id).flux(); 
    }
    
    // Yeni bir kitap oluştur
    @PostMapping
    public Flux<Book> create(@RequestBody Book b) {
        return tools.createBook(b.getTitle(), b.getAuthor(), b.getIsbn())
                .doOnNext(sink::tryEmitNext)  // Abonelere bildir
                .flux();
    }
    
    // Mevcut bir kitabı güncelle
    @PutMapping("/{id}")
    public Flux<Book> update(@PathVariable Long id, @RequestBody Book b) {
        return tools.updateBook(id, b.getTitle(), b.getAuthor(), b.getIsbn())
                .doOnNext(sink::tryEmitNext)  // Abonelere bildir
                .flux();
    }
    
    // Bir kitabı sil
    @DeleteMapping("/{id}")
    public Flux<Void> delete(@PathVariable Long id) {
        return tools.deleteBook(id)
                .doOnTerminate(() -> 
                    sink.tryEmitNext(new Book("deleted#" + id, "", "")))
                .flux();
    }
}
```

**Temel Özellikler:**
1.  **Gerçek Zamanlı Güncellemeler**: `/stream` endpoint'i, kitaplar her değiştiğinde güncelleme gönderir.
2.  **RESTful Endpoints**: Standart CRUD operasyonları (GET, POST, PUT, DELETE).
3.  **Reaktif Programlama**: Bloke etmeyen operasyonlar için Project Reactor kullanır.
4.  **WebSocket Benzeri Davranış**: Gerçek zamanlı güncellemeler için Server-Sent Events (SSE) kullanır.

### 4.5 MCP Controller (McpServerController.java)
Bu, AI sistemlerinin kitap sistemimizle MCP protokolünü kullanarak etkileşime girmesini sağlayan özel bir controller'dır.

MCP sunucumuzun çekirdeği üç ana istek türünü yönetir:

1.  **initialize**: Başlangıç el sıkışmasını (handshake) yönetir.
2.  **tools/list**: Mevcut tüm araçları listeler.
3.  **tools/call**: Belirli bir aracı çalıştırır.

## 5. Kendi MCP Sunucunuzu Nasıl Uygularsınız?

### 5.1 Bağımlılıkları Ayarlama
Bunları `pom.xml` dosyanıza ekleyin:
```xml
<dependencies>
    <!-- Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- Veritabanı -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    
    <!-- MCP Sunucusu -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-mcp-server-spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

### 5.2 Entity Sınıfınızı Oluşturma
```java
@Entity
public class YourEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    // diğer alanlar, getter'lar, setter'lar
}
```

### 5.3 Repository Oluşturma
```java
@Repository
public interface YourRepository extends JpaRepository<YourEntity, Long> {
    // Özel sorgu metotları
}
```

### 5.4 Servis Katmanı Oluşturma
```java
@Service
public class YourService {
    private final YourRepository repo;
    
    public YourService(YourRepository repo) {
        this.repo = repo;
    }
    
    // İş mantığı metotlarınız
}
```

### 5.5 MCP Controller'ı Uygulama
```java
@RestController
public class YourMcpController {
    private final YourService service;
    private final ObjectMapper om = new ObjectMapper();
    
    @PostMapping("/mcp")
    public Mono<Map<String, Object>> handleRequest(@RequestBody Map<String, Object> request) {
        String method = (String) request.get("method");
        Object id = request.get("id");
        
        return switch (method) {
            case "initialize" -> handleInitialize(id);
            case "tools/list" -> handleToolsList(id);
            case "tools/call" -> handleToolsCall(id, (Map<String, Object>) request.get("params"));
            default -> Mono.just(createError(id, -32601, "Metot bulunamadı"));
        };
    }
    
    // Handler metotlarını uygulayın...
}
```

## 6. Test Etme ve Dağıtım

### 6.1 Test Etme
1.  Endpoint'leri test etmek için Postman veya `curl` kullanın.
2.  Kitapları listelemek için örnek test:
```bash
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tools/call",
    "params": {
      "name": "getAllBooks"
    }
  }'
```

### 6.2 Dağıtım
1.  Projeyi derleyin:
```bash
./mvnw clean package
```
2.  Uygulamayı çalıştırın:
```bash
java -jar target/your-application.jar
```

## 7. En İyi Uygulamalar

1.  **Hata Yönetimi**: Her zaman uygun hata yönetimini dahil edin.
2.  **Doğrulama**: Tüm girdileri doğrulayın.
3.  **Dokümantasyon**: API'nizi ve araçlarınızı belgeleyin.
4.  **Güvenlik**: Uygun kimlik doğrulama ve yetkilendirme uygulayın.
5.  **Loglama**: Kapsamlı loglama ekleyin.
6.  **Test**: Birim ve entegrasyon testleri yazın.

## 8. Sık Karşılaşılan Sorunlar ve Çözümleri

1.  **JSON-RPC Hataları**:
    -   Kod -32600: Geçersiz İstek (Invalid Request)
    -   Kod -32601: Metot Bulunamadı (Method not found)
    -   Kod -32602: Geçersiz Parametreler (Invalid params)
    -   Kod -32603: İç Hata (Internal error)
2.  **Veritabanı Bağlantı Sorunları**:
    -   `application.properties` dosyasındaki veritabanı URL'sini kontrol edin.
    -   Veritabanı kimlik bilgilerini doğrulayın.
    -   Veritabanı sunucusunun çalıştığından emin olun.
3.  **MCP Protokolü Sorunları**:
    -   İsteklerde tüm gerekli alanların bulunduğundan emin olun.
    -   JSON formatını doğrulayın.
    -   Metot adlarını ve parametreleri kontrol edin.

## 9. Mevcut Bir Sisteme Swagger ile MCP Uygulamak

Eğer zaten Swagger dokümantasyonuna sahip bir Spring Boot uygulamanız varsa, MCP desteği eklemek oldukça basittir.

### 9.1 Ön Koşullar
- Mevcut bir Spring Boot uygulaması
- Swagger dokümantasyonu için Springfox veya SpringDoc OpenAPI
- API endpoint'leriniz hakkında temel bilgi

### 9.2 Adım 1: Gerekli Bağımlılıkları Ekleme
Bunları `pom.xml` dosyanıza ekleyin:
```xml
<!-- MCP Sunucusu -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-mcp-server-spring-boot-starter</artifactId>
    <version>0.8.0</version> <!-- En son sürümü kullanın -->
</dependency>

<!-- Eğer WebFlux yoksa -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

### 9.3 Adım 2: MCP Konfigürasyonu Oluşturma
MCP desteğini etkinleştirmek için bir konfigürasyon sınıfı oluşturun:
```java
@Configuration
public class McpConfig {
    
    @Bean
    public McpServerProperties mcpServerProperties() {
        McpServerProperties properties = new McpServerProperties();
        properties.setEnabled(true);
        return properties;
    }
    
    @Bean
    public RouterFunction<ServerResponse> mcpRouter(McpHandler mcpHandler) {
        return RouterFunctions
            .route(POST("/mcp"), mcpHandler::handleRequest);
    }
}
```

### 9.4 Adım 3: MCP Handler Oluşturma
Bu sınıf, MCP protokol mesajlarını yönetecektir:
```java
@Component
@RequiredArgsConstructor
public class McpHandler {
    
    private final ObjectMapper objectMapper;
    private final YourApiService apiService;
    
    public Mono<ServerResponse> handleRequest(ServerRequest request) {
        return request.bodyToMono(Map.class)
            .flatMap(this::processRequest)
            .onErrorResume(this::handleError);
    }
    
    private Mono<ServerResponse> processRequest(Map<String, Object> request) {
        String method = (String) request.get("method");
        Object id = request.get("id");
        
        return switch (method) {
            case "initialize" -> handleInitialize(id);
            case "tools/list" -> handleListTools(id);
            case "tools/call" -> handleToolCall(id, (Map<String, Object>) request.get("params"));
            default -> handleMethodNotFound(id, method);
        };
    }
    
    // ... handler metotları ...
}
```

### 9.5 Adım 4: Swagger'ı MCP Araçlarına Eşleme
Swagger endpoint'lerinizi MCP araçlarına dönüştürün:
```java
private Mono<ServerResponse> handleListTools(Object id) {
    // OpenAPI dokümantasyonunuzu alın
    OpenAPI openApi = openApiResource.getOpenApi();
    
    List<Map<String, Object>> tools = new ArrayList<>();
    
    // Her API yolunu bir MCP aracına dönüştürün
    openApi.getPaths().forEach((path, pathItem) -> {
        pathItem.readOperations().forEach(operation -> {
            Map<String, Object> tool = new HashMap<>();
            tool.put("name", operation.getOperationId());
            tool.put("description", operation.getDescription());
            
            // Parametreleri ekleyin
            List<Map<String, Object>> params = new ArrayList<>();
            if (operation.getParameters() != null) {
                operation.getParameters().forEach(param -> {
                    Map<String, Object> paramMap = new HashMap<>();
                    paramMap.put("name", param.getName());
                    paramMap.put("description", param.getDescription());
                    paramMap.put("required", param.getRequired());
                    params.add(paramMap);
                });
            }
            tool.put("parameters", params);
            
            tools.add(tool);
        });
    });
    
    return createSuccessResponse(id, Map.of("tools", tools));
}
```

### 9.6 Adım 5: Araç Çalıştırmayı Yönetme
Araç çalıştırma mantığını uygulayın:
```java
private Mono<ServerResponse> handleToolCall(Object id, Map<String, Object> params) {
    String toolName = (String) params.get("name");
    Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");
    
    return switch (toolName) {
        case "getBookById" -> {
            Long bookId = Long.parseLong(arguments.get("id").toString());
            yield apiService.getBookById(bookId)
                .flatMap(book -> createSuccessResponse(id, book));
        }
        // Gerektiğinde daha fazla araç durumu ekleyin
        default -> handleMethodNotFound(id, toolName);
    };
}
```

### 9.7 Adım 6: MCP Sunucunuzu Test Etme
MCP sunucunuzu `curl` veya Postman kullanarak test edin:
```bash
# Mevcut araçları listele
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tools/list"
  }'

# Bir aracı çağır
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 2,
    "method": "tools/call",
    "params": {
      "name": "getBookById",
      "arguments": {
        "id": "123"
      }
    }
  }'
```

### 9.8 Swagger ile MCP için En İyi Uygulamalar
- **Araç Adlarını Tutarlı Tutun**: Swagger `operationId`'lerinizle aynı isimleri kullanın.
- **Detaylıca Belgeleyin**: Swagger dokümantasyonunuzun eksiksiz olduğundan emin olun.
- **Hataları Zarifçe Yönetin**: Anlamlı hata mesajları sağlayın.
- **API'nizi Sürümleyin**: MCP endpoint'lerinize sürümleme ekleyin.
- **Endpoint'lerinizi Güvenceye Alın**: Kimlik doğrulama/yetkilendirme ekleyin.

### 9.9 Sık Karşılaşılan Sorunlar ve Çözümleri
- **Eksik Bağımlılıklar**: Gerekli tüm bağımlılıkların dahil edildiğinden emin olun.
- **Sürüm Uyuşmazlıkları**: Spring AI ve ilgili kütüphaneleri uyumlu tutun.
- **CORS Sorunları**: Çapraz kaynak istekleri için CORS'u doğru şekilde yapılandırın.
- **Serileştirme Hataları**: Modellerinizin JSON'a doğru şekilde serileştirilebildiğinden emin olun.

## Sonuç

Bu rehber, size sıfırdan bir MCP sunucusu oluşturma sürecinde yol gösterdi. Bu kitap takip uygulamasında gösterilen ilkeler herhangi bir alana uygulanabilir. Önemli olanlar şunlardır:

1.  Veri modelinizi tanımlayın.
2.  Veri erişimi için repository'ler oluşturun.
3.  İş mantığını servislerde uygulayın.
4.  İşlevselliği MCP endpoint'leri aracılığıyla dışa açın.
5.  Kapsamlı bir şekilde test edin.
6.  Dağıtın ve izleyin.

Bu adımları izleyerek, AI modellerinin uygulamanızla standartlaştırılmış ve güvenli bir şekilde etkileşime girmesini sağlayabilirsiniz.