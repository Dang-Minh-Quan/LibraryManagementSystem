package com.library_management.auth;

import com.library_management.security.JwtConfiguration;
import com.library_management.security.JwtProperties;
import com.library_management.user.Role;
import com.library_management.user.User;
import com.library_management.user.persistence.JpaUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AuthSecurityIntegrationTest.SecurityProbeController.class)
class AuthSecurityIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JpaUserRepository users;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtEncoder encoder;
    @Autowired JwtDecoder decoder;
    @Autowired JwtProperties properties;

    @BeforeEach
    void clearUsers() { users.deleteAll(); }

    @Test
    void registerLoginAndGetOwnProfile() throws Exception {
        MvcResult registered = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("reader", "strong-password")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.username").value("reader"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.password").doesNotExist()).andReturn();
        assertThat(registered.getRequest().getSession(false)).isNull();
        User stored = users.findByUsername("reader").orElseThrow();
        assertThat(stored.getPassword()).isNotEqualTo("strong-password");
        assertThat(passwords.matches("strong-password", stored.getPassword())).isTrue();

        String token = login("reader", "strong-password");
        Jwt jwt = decoder.decode(token);
        assertThat(jwt.getSubject()).isEqualTo(stored.getId().toString());
        assertThat(jwt.getExpiresAt()).isAfter(jwt.getIssuedAt());
        assertThat(jwt.getClaims()).doesNotContainKeys("password", "passwordHash");
        MvcResult profile = mvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(stored.getId()))
                .andExpect(jsonPath("$.username").value("reader"))
                .andExpect(jsonPath("$.password").doesNotExist()).andReturn();
        assertThat(profile.getRequest().getSession(false)).isNull();
    }

    @Test
    void registrationIgnoresClientProvidedRole() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"reader\",\"password\":\"secret123\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("USER"));
        assertThat(users.findByUsername("reader").orElseThrow().getRole()).isEqualTo(Role.USER);
    }

    @Test
    void duplicateUsernameReturnsSafeBadRequest() throws Exception {
        createUser("reader", Role.USER);
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("reader", "secret123")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Username already exists"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
        assertThat(users.count()).isEqualTo(1);
    }

    @Test
    void concurrentRegistrationsCreateOnlyOneUser() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Integer> register = () -> {
            ready.countDown();
            if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Start timed out");
            return mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                    .content(credentials("concurrent", "secret123"))).andReturn().getResponse().getStatus();
        };
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(register);
            var second = executor.submit(register);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 400);
        }
        assertThat(users.count()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}", "{\"username\":\"ab\",\"password\":\"secret123\"}",
            "{\"username\":\"bad name\",\"password\":\"secret123\"}",
            "{\"username\":\"reader\",\"password\":\"123\"}",
            "{\"username\":\"reader\",\"password\":\"      \"}", "not-json"
    })
    void invalidRegistrationHasConsistentError(String body) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").isNotEmpty()).andExpect(jsonPath("$.timestamp").isNotEmpty());
        assertThat(users.count()).isZero();
    }

    @Test
    void bcryptByteLimitIsValidatedBeforeHashing() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("reader", "\u20ac".repeat(25))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        assertThat(users.count()).isZero();
    }

    @Test
    void wrongPasswordAndUnknownUserHaveSameError() throws Exception {
        createUser("reader", Role.USER);
        for (String name : List.of("reader", "missing")) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(credentials(name, "wrong-password")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Invalid username or password"))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.timestamp").isNotEmpty());
        }
    }

    @Test
    void anonymousRequestsCannotAccessProtectedEndpoints() throws Exception {
        mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.status").value(401)).andExpect(jsonPath("$.timestamp").isNotEmpty());
        mvc.perform(post("/api/books")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/books/1")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/borrowings")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/borrowings/me")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/borrowings/1/return")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void authenticatedAccountsCanManageBooksAndBorrowRegardlessOfStoredRole(Role role) throws Exception {
        createUser("reader", role);
        String token = login("reader", "secret123");
        mvc.perform(post("/api/books").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.securityProbe").value(true));
        mvc.perform(delete("/api/books/1").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.securityProbe").value(true));
        mvc.perform(post("/api/borrowings").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.securityProbe").value(true));
        mvc.perform(get("/api/borrowings/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
        mvc.perform(post("/api/borrowings/1/return").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void accessDeniedErrorsKeepConsistentJsonWithoutRoleRestrictions() throws Exception {
        createUser("reader", Role.USER);
        String token = login("reader", "secret123");
        mvc.perform(get("/api/test/forbidden").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void deletedAccountCannotUseExistingToken() throws Exception {
        User user = createUser("reader", Role.USER);
        String token = login("reader", "secret123");
        users.deleteById(user.getId());
        assertInvalidToken(token);
    }

    @Test
    void malformedAndTamperedTokensAreRejected() throws Exception {
        createUser("reader", Role.USER);
        String token = login("reader", "secret123");
        assertInvalidToken("not-a-jwt");
        String[] parts = token.split("\\.");
        char replacement = parts[2].charAt(0) == 'A' ? 'B' : 'A';
        assertInvalidToken(parts[0] + "." + parts[1] + "." + replacement + parts[2].substring(1));
    }

    @Test
    void expiredFutureWrongIssuerAudienceAndMissingClaimsAreRejected() throws Exception {
        User user = createUser("reader", Role.USER);
        Instant now = Instant.now();
        assertInvalidToken(sign(claims(user).issuedAt(now.minusSeconds(600)).expiresAt(now.minusSeconds(300))));
        assertInvalidToken(sign(claims(user).notBefore(now.plusSeconds(300))));
        assertInvalidToken(sign(claims(user).issuer("other-issuer")));
        assertInvalidToken(sign(claims(user).audience(List.of("another-api"))));
        assertInvalidToken(sign(claims(user).subject("not-a-user-id")));
        assertInvalidToken(sign(claims(user).subject("99999999999999999999999999")));
        assertInvalidToken(sign(JwtClaimsSet.builder().issuer(properties.issuer())
                .audience(List.of(JwtConfiguration.AUDIENCE)).subject(user.getId().toString())));
    }

    @Test
    void publicBooksAndOpenApiAreAccessibleWithoutLogin() throws Exception {
        mvc.perform(get("/api/books")).andExpect(status().isOk());
        mvc.perform(get("/api/books/1")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.paths['/api/auth/register'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/users/me'].get.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.components.schemas.ApiError.properties.timestamp").exists());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    private User createUser(String username, Role role) {
        return users.saveAndFlush(new User(username, passwords.encode("secret123"), role));
    }

    private String credentials(String username, String password) {
        return mapper.writeValueAsString(Map.of("username", username, "password", password));
    }

    private String login(String username, String password) throws Exception {
        MvcResult response = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(username, password)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty()).andReturn();
        return mapper.readTree(response.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private JwtClaimsSet.Builder claims(User user) {
        Instant now = Instant.now();
        return JwtClaimsSet.builder().issuer(properties.issuer()).audience(List.of(JwtConfiguration.AUDIENCE))
                .subject(user.getId().toString()).issuedAt(now).expiresAt(now.plusSeconds(3600));
    }

    private String sign(JwtClaimsSet.Builder claims) {
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims.build()))
                .getTokenValue();
    }

    private void assertInvalidToken(String token) throws Exception {
        mvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    /** Test-only endpoints verify the filter rules before Book/Borrowing modules are merged. */
    @RestController
    static class SecurityProbeController {
        @GetMapping("/api/test/forbidden")
        @PreAuthorize("denyAll()")
        public Map<String, Boolean> forbiddenProbe() { return Map.of("securityProbe", true); }

        @GetMapping({"/api/books", "/api/books/{id}", "/api/borrowings/me"})
        Map<String, Boolean> getProbe() { return Map.of("securityProbe", true); }
        @PostMapping({"/api/books", "/api/borrowings", "/api/borrowings/{id}/return"})
        Map<String, Boolean> postProbe() { return Map.of("securityProbe", true); }
        @DeleteMapping("/api/books/{id}")
        Map<String, Boolean> deleteProbe() { return Map.of("securityProbe", true); }
    }
}
