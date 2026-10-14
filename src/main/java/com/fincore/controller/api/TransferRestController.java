package com.fincore.controller.api;

import com.fincore.dto.TransferRequestDto;
import com.fincore.dto.TransferResponseDto;
import com.fincore.service.TransferService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferRestController {

    private final TransferService transferService;

    public TransferRestController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    public ResponseEntity<TransferResponseDto> initiateTransfer(
            @Valid @RequestBody TransferRequestDto requestDto,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKeyHeader,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request) {

        // Header takes precedence if provided
        if (idempotencyKeyHeader != null && !idempotencyKeyHeader.isBlank()) {
            requestDto.setIdempotencyKey(idempotencyKeyHeader.trim());
        }

        TransferResponseDto response = transferService.executeTransfer(
                requestDto,
                userDetails.getUsername(),
                request.getRemoteAddr()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
