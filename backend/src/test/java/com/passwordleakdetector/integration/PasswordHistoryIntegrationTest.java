package com.passwordleakdetector.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.passwordleakdetector.dto.auth.LoginRequest;
import com.passwordleakdetector.dto.auth.RegisterRequest;
import com.passwordleakdetector.dto.password.HistoryEntryRequest;
import com.passwordleakdetector.entity.PasswordHistoryEntry;
import com.passwordleakdetector.repository.PasswordHistoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PasswordHistoryIntegrationTest {

    @Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordHistoryRepository historyRepository;

    private String registerAndLogin(String username) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(username, "correct-horse-battery-staple"))))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(username, "correct-horse-battery-staple"))))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("accessToken").asText();
    }

    @Test
    void crudIsScopedToTheOwningUserAndPasswordIsEncryptedAtRest() throws Exception {
        String suffix = System.nanoTime() + "";
        String accessTokenA = registerAndLogin("owner_" + suffix);
        String accessTokenB = registerAndLogin("intruder_" + suffix);

        HistoryEntryRequest createRequest = new HistoryEntryRequest(
                "example.com", "https://example.com", "owner@example.com", "s3cret-site-password!", "personal account");

        MvcResult createResult = mockMvc.perform(post("/api/v1/passwords/history")
                        .header("Authorization", "Bearer " + accessTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        assertThat(created.has("password")).isFalse();
        long entryId = created.get("id").asLong();

        // list is metadata-only
        MvcResult listResult = mockMvc.perform(get("/api/v1/passwords/history")
                        .header("Authorization", "Bearer " + accessTokenA))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode list = objectMapper.readTree(listResult.getResponse().getContentAsString());
        assertThat(list).hasSize(1);
        assertThat(list.get(0).has("password")).isFalse();

        // owner can fetch the decrypted password for a single entry
        MvcResult detailResult = mockMvc.perform(get("/api/v1/passwords/history/" + entryId)
                        .header("Authorization", "Bearer " + accessTokenA))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode detail = objectMapper.readTree(detailResult.getResponse().getContentAsString());
        assertThat(detail.get("password").asText()).isEqualTo("s3cret-site-password!");

        // the raw DB row never holds the plaintext password
        PasswordHistoryEntry stored = historyRepository.findById(entryId).orElseThrow();
        assertThat(stored.getEncryptedPassword()).isNotEqualTo("s3cret-site-password!");
        assertThat(stored.getEncryptedPassword()).doesNotContain("s3cret-site-password!");

        // a different user cannot read, update or delete this entry - 404, not 403
        mockMvc.perform(get("/api/v1/passwords/history/" + entryId)
                        .header("Authorization", "Bearer " + accessTokenB))
                .andExpect(status().isNotFound());

        HistoryEntryRequest intruderUpdate = new HistoryEntryRequest(
                "hacked.com", null, null, "new-password", null);
        mockMvc.perform(put("/api/v1/passwords/history/" + entryId)
                        .header("Authorization", "Bearer " + accessTokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(intruderUpdate)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/passwords/history/" + entryId)
                        .header("Authorization", "Bearer " + accessTokenB))
                .andExpect(status().isNotFound());

        // owner can update their own entry
        HistoryEntryRequest ownerUpdate = new HistoryEntryRequest(
                "example.com", "https://example.com", "owner@example.com", "new-rotated-password", "updated");
        mockMvc.perform(put("/api/v1/passwords/history/" + entryId)
                        .header("Authorization", "Bearer " + accessTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ownerUpdate)))
                .andExpect(status().isOk());

        MvcResult updatedDetail = mockMvc.perform(get("/api/v1/passwords/history/" + entryId)
                        .header("Authorization", "Bearer " + accessTokenA))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(objectMapper.readTree(updatedDetail.getResponse().getContentAsString()).get("password").asText())
                .isEqualTo("new-rotated-password");

        // owner can delete their own entry
        mockMvc.perform(delete("/api/v1/passwords/history/" + entryId)
                        .header("Authorization", "Bearer " + accessTokenA))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/passwords/history/" + entryId)
                        .header("Authorization", "Bearer " + accessTokenA))
                .andExpect(status().isNotFound());
    }
}
