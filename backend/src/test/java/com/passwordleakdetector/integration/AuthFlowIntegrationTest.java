package com.passwordleakdetector.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.passwordleakdetector.dto.auth.LoginRequest;
import com.passwordleakdetector.dto.auth.RegisterRequest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerLoginRefreshRotationReuseDetectionAndLogout() throws Exception {
        String username = "alice_" + System.nanoTime();

        // register
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(username, "correct-horse-battery-staple"))))
                .andExpect(status().isCreated());

        // duplicate register is rejected
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(username, "correct-horse-battery-staple"))))
                .andExpect(status().isConflict());

        // wrong password is rejected
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(username, "totally-wrong-password"))))
                .andExpect(status().isUnauthorized());

        // login succeeds
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(username, "correct-horse-battery-staple"))))
                .andExpect(status().isOk())
                .andReturn();

        String accessToken1 = extractField(loginResult, "accessToken");
        Cookie refreshCookie1 = loginResult.getResponse().getCookie("refreshToken");
        assertThat(refreshCookie1).isNotNull();
        assertThat(refreshCookie1.isHttpOnly()).isTrue();

        // protected endpoint rejects unauthenticated requests
        mockMvc.perform(get("/api/v1/passwords/history"))
                .andExpect(status().isUnauthorized());

        // protected endpoint accepts a valid bearer token
        mockMvc.perform(get("/api/v1/passwords/history")
                        .header("Authorization", "Bearer " + accessToken1))
                .andExpect(status().isOk());

        // refresh rotates the token
        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie1))
                .andExpect(status().isOk())
                .andReturn();
        String accessToken2 = extractField(refreshResult, "accessToken");
        Cookie refreshCookie2 = refreshResult.getResponse().getCookie("refreshToken");
        assertThat(refreshCookie2).isNotNull();
        assertThat(refreshCookie2.getValue()).isNotEqualTo(refreshCookie1.getValue());
        assertThat(accessToken2).isNotBlank();

        // reusing the now-revoked original refresh token is rejected and revokes the whole family
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie1))
                .andExpect(status().isUnauthorized());

        // the rotated (child) token is now also revoked as a result of the reuse detection above
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie2))
                .andExpect(status().isUnauthorized());

        // a fresh login + logout invalidates that new refresh token too
        MvcResult secondLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(username, "correct-horse-battery-staple"))))
                .andExpect(status().isOk())
                .andReturn();
        Cookie refreshCookie3 = secondLogin.getResponse().getCookie("refreshToken");

        mockMvc.perform(post("/api/v1/auth/logout").cookie(refreshCookie3))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie3))
                .andExpect(status().isUnauthorized());
    }

    private String extractField(MvcResult result, String field) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get(field).asText();
    }
}
