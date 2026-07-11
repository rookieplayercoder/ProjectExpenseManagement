package com.prateek.ProjectExpenseManagement.controller;

import com.prateek.ProjectExpenseManagement.dto.SettleBalanceRequest;
import com.prateek.ProjectExpenseManagement.service.SettlementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/settlements")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> settleBalance(
            @Valid @RequestBody SettleBalanceRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        UUID settlementId = settlementService.settleBalance(request, idempotencyKey);
        return Map.of(
                "settlementId", settlementId,
                "status", "SUCCESS",
                "message", "Settlement recorded successfully"
        );
    }
}
