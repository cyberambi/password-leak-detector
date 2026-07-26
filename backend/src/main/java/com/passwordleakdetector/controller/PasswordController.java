package com.passwordleakdetector.controller;

import com.passwordleakdetector.dto.password.BreachCheckResponse;
import com.passwordleakdetector.dto.password.GeneratePasswordRequest;
import com.passwordleakdetector.dto.password.GeneratePasswordResponse;
import com.passwordleakdetector.dto.password.HistoryEntryDetailResponse;
import com.passwordleakdetector.dto.password.HistoryEntryRequest;
import com.passwordleakdetector.dto.password.HistoryEntryResponse;
import com.passwordleakdetector.dto.password.PasswordValueRequest;
import com.passwordleakdetector.dto.password.StrengthAnalysisResponse;
import com.passwordleakdetector.security.UserPrincipal;
import com.passwordleakdetector.service.BreachCheckService;
import com.passwordleakdetector.service.PasswordGeneratorService;
import com.passwordleakdetector.service.PasswordHistoryService;
import com.passwordleakdetector.service.PasswordStrengthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/passwords")
public class PasswordController {

    private final BreachCheckService breachCheckService;
    private final PasswordStrengthService strengthService;
    private final PasswordGeneratorService generatorService;
    private final PasswordHistoryService historyService;

    public PasswordController(BreachCheckService breachCheckService,
                               PasswordStrengthService strengthService,
                               PasswordGeneratorService generatorService,
                               PasswordHistoryService historyService) {
        this.breachCheckService = breachCheckService;
        this.strengthService = strengthService;
        this.generatorService = generatorService;
        this.historyService = historyService;
    }

    @PostMapping("/check-breach")
    public BreachCheckResponse checkBreach(@Valid @RequestBody PasswordValueRequest request) {
        return breachCheckService.check(request.password());
    }

    @PostMapping("/analyze-strength")
    public StrengthAnalysisResponse analyzeStrength(@Valid @RequestBody PasswordValueRequest request) {
        return strengthService.analyze(request.password());
    }

    @PostMapping("/generate")
    public GeneratePasswordResponse generate(@Valid @RequestBody GeneratePasswordRequest request) {
        return new GeneratePasswordResponse(generatorService.generate(request));
    }

    @GetMapping("/history")
    public List<HistoryEntryResponse> listHistory(@AuthenticationPrincipal UserPrincipal principal) {
        return historyService.list(principal.getId());
    }

    @GetMapping("/history/{id}")
    public HistoryEntryDetailResponse getHistoryEntry(@PathVariable Long id,
                                                       @AuthenticationPrincipal UserPrincipal principal) {
        return historyService.getWithPassword(id, principal.getId());
    }

    @PostMapping("/history")
    public ResponseEntity<HistoryEntryResponse> createHistoryEntry(@Valid @RequestBody HistoryEntryRequest request,
                                                                     @AuthenticationPrincipal UserPrincipal principal) {
        HistoryEntryResponse response = historyService.create(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/history/{id}")
    public HistoryEntryResponse updateHistoryEntry(@PathVariable Long id,
                                                    @Valid @RequestBody HistoryEntryRequest request,
                                                    @AuthenticationPrincipal UserPrincipal principal) {
        return historyService.update(id, principal.getId(), request);
    }

    @DeleteMapping("/history/{id}")
    public ResponseEntity<Void> deleteHistoryEntry(@PathVariable Long id,
                                                    @AuthenticationPrincipal UserPrincipal principal) {
        historyService.delete(id, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
