package com.fisbu.api.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fisbu.api.repository.UserRepository;

/**
 * E2E-001: "kayıt ol -> login -> fiş oluştur -> kategoriye ata -> bütçe listesinde
 * görünüyor mu" akışının uçtan uca (gerçek HTTP + gerçek Postgres, hiçbir katman
 * mock'lanmadan) doğrulaması.
 *
 * <p>Testcontainers ile ephemeral (test bitince silinen) bir Postgres ayağa kalkar;
 * canlı Supabase'e KESİNLİKLE bağlanılmaz. Bu sınıf argümansız {@code ./gradlew test}
 * ile ÇALIŞTIRILMAZ — sadece {@code ./gradlew e2eTest} ile.</p>
 *
 * <p>Her HTTP adımının süresi ölçülüp loglanır — E2E-001'in "anormal gecikme/donma"
 * gözlemleme gerekliliği için. Bir adım {@link #SLOW_STEP_THRESHOLD_MS}'i aşarsa test
 * başarısız OLMAZ (bu bir smoke/perf-gözlem testi, katı bir SLA testi değil) ama WARN
 * seviyesinde loglanır — insan gözden geçirmesi için.</p>
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RegisterToBudgetFlowE2ETest {

    private static final Logger log = LoggerFactory.getLogger(RegisterToBudgetFlowE2ETest.class);
    private static final long SLOW_STEP_THRESHOLD_MS = 3000;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("fisbu_e2e")
            .withUsername("fisbu")
            .withPassword("fisbu_e2e_password");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // Flyway V1/V2/V3 migration'ları bu taze DB'ye GERÇEKTEN uygulanır (baseline yok) —
        // schema'nın sıfırdan doğru kurulduğunu da bu E2E dolaylı olarak kanıtlar.
        registry.add("spring.flyway.baseline-on-migrate", () -> "false");
        // Gerçek ağ isteği yapan servisler devre dışı/dummy — E2E'nin amacı bunları test etmek değil
        registry.add("jwt.secret", () -> "e2e-test-only-secret-key-must-be-at-least-256-bits-long");
        registry.add("cloudinary.cloud-name", () -> "e2e-dummy");
        registry.add("cloudinary.api-key", () -> "e2e-dummy");
        registry.add("cloudinary.api-secret", () -> "e2e-dummy");
        registry.add("brevo.api-key", () -> "");
        registry.add("groq.api-key", () -> "");
        registry.add("firebase.service-account-json", () -> "");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    private static String jwtToken;
    private static Long categoryId;
    private static Long budgetId;
    private static final String EMAIL = "e2e-" + System.currentTimeMillis() + "@fisbu-test.com";
    private static final String PASSWORD = "E2eTestPass1!";
    private static final LocalDate RECEIPT_DATE = LocalDate.now();

    private <T> T timed(String stepLabel, Supplier<T> call) {
        long start = System.nanoTime();
        T result = call.get();
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        if (elapsedMs > SLOW_STEP_THRESHOLD_MS) {
            log.warn("[E2E-PERF] '{}' adımı {} ms sürdü (eşik: {} ms) — ANORMAL GECİKME", stepLabel, elapsedMs, SLOW_STEP_THRESHOLD_MS);
        } else {
            log.info("[E2E-PERF] '{}' adımı {} ms sürdü", stepLabel, elapsedMs);
        }
        return result;
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        return headers;
    }

    @Test
    @Order(1)
    void adim1_kayitOlVeEmailDogrula() {
        Map<String, String> registerBody = Map.of(
                "email", EMAIL, "password", PASSWORD, "name", "E2E Test Kullanıcı");

        ResponseEntity<Map> registerResponse = timed("POST /auth/register",
                () -> restTemplate.postForEntity("/auth/register", registerBody, Map.class));
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Doğrulama kodu e-posta ile gönderiliyor (gerçek e-posta gönderilmiyor, brevo.api-key
        // boş) — testin kendisi, gerçek bir kullanıcının erişemeyeceği DB'ye erişimle kodu okur.
        String verificationCode = userRepository.findByEmail(EMAIL)
                .orElseThrow(() -> new AssertionError("Kullanıcı DB'de bulunamadı"))
                .getVerificationCode();
        assertThat(verificationCode).isNotBlank();

        Map<String, String> verifyBody = Map.of("email", EMAIL, "code", verificationCode);
        ResponseEntity<Void> verifyResponse = timed("POST /auth/verify-email",
                () -> restTemplate.postForEntity("/auth/verify-email", verifyBody, Void.class));
        assertThat(verifyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @Order(2)
    void adim2_loginOlVeTokenAl() {
        Map<String, String> loginBody = Map.of("email", EMAIL, "password", PASSWORD);

        ResponseEntity<Map> loginResponse = timed("POST /auth/login",
                () -> restTemplate.postForEntity("/auth/login", loginBody, Map.class));

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        jwtToken = (String) loginResponse.getBody().get("token");
        assertThat(jwtToken).isNotBlank();
    }

    @Test
    @Order(3)
    void adim3_kategoriOlustur() {
        Map<String, String> categoryBody = Map.of("name", "E2E Market", "color", "#4CAF50");
        HttpEntity<Map<String, String>> request = new HttpEntity<>(categoryBody, authHeaders());

        ResponseEntity<Map> response = timed("POST /categories",
                () -> restTemplate.postForEntity("/categories", request, Map.class));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        categoryId = ((Number) response.getBody().get("id")).longValue();
        assertThat(categoryId).isPositive();
    }

    @Test
    @Order(4)
    void adim4_kategoriIcinButceOlustur() {
        Map<String, Object> budgetBody = Map.of(
                "categoryId", categoryId,
                "monthlyLimit", new BigDecimal("1000.00"),
                "year", RECEIPT_DATE.getYear(),
                "month", RECEIPT_DATE.getMonthValue());
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(budgetBody, authHeaders());

        ResponseEntity<Map> response = timed("POST /budgets",
                () -> restTemplate.postForEntity("/budgets", request, Map.class));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        budgetId = ((Number) response.getBody().get("id")).longValue();
        assertThat(new BigDecimal(response.getBody().get("currentSpend").toString()))
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @Order(5)
    void adim5_kategoriliFisOlustur() {
        Map<String, Object> receiptBody = Map.of(
                "storeName", "E2E Migros",
                "totalAmount", new BigDecimal("150.50"),
                "receiptDate", RECEIPT_DATE.toString(),
                "categoryId", categoryId,
                "allowDuplicate", true);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(receiptBody, authHeaders());

        ResponseEntity<Map> response = timed("POST /receipts",
                () -> restTemplate.postForEntity("/receipts", request, Map.class));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().get("categoryName")).isEqualTo("E2E Market");
    }

    @Test
    @Order(6)
    void adim6_fisinButceListesindeYansidiginiDogrula() {
        HttpEntity<Void> request = new HttpEntity<>(authHeaders());

        ResponseEntity<List> response = timed("GET /budgets",
                () -> restTemplate.exchange(
                        "/budgets?year=" + RECEIPT_DATE.getYear() + "&month=" + RECEIPT_DATE.getMonthValue(),
                        org.springframework.http.HttpMethod.GET, request, List.class));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> budgets = response.getBody();
        Map<String, Object> ourBudget = budgets.stream()
                .filter(b -> budgetId.equals(((Number) b.get("id")).longValue()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Oluşturulan bütçe /budgets listesinde görünmüyor"));

        // Kritik doğrulama: fiş bütçeye YANSIDI — bu, receipt->budget event-driven
        // entegrasyonunun (ARCH-001'de kurulan) uçtan uca çalıştığının kanıtı.
        assertThat(new BigDecimal(ourBudget.get("currentSpend").toString()))
                .isEqualByComparingTo(new BigDecimal("150.50"));

        assertThatCode(() -> log.info("[E2E] Tam akış başarılı: kayıt->doğrulama->login->kategori->bütçe->fiş->bütçede-yansıma"))
                .doesNotThrowAnyException();
    }
}
