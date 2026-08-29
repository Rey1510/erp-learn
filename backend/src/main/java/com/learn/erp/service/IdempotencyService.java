package com.learn.erp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learn.erp.model.IdempotencyRecord;
import com.learn.erp.repository.IdempotencyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class IdempotencyService {

    private final IdempotencyRepository idempotencyRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public IdempotencyService(IdempotencyRepository idempotencyRepository, ObjectMapper objectMapper) {
        this.idempotencyRepository = idempotencyRepository;
        this.objectMapper = objectMapper;
    }

    public String computeHash(Object request) {
        try {
            String json = objectMapper.writeValueAsString(request);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return "NO_HASH_" + System.currentTimeMillis();
        }
    }

    public Optional<IdempotencyRecord> findRecord(String key) {
        if (key == null || key.isBlank()) return Optional.empty();
        return idempotencyRepository.findByIdempotencyKey(key.trim());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IdempotencyRecord startRequest(String key, String endpoint, String requestHash) {
        IdempotencyRecord record = new IdempotencyRecord(key.trim(), endpoint, requestHash);
        return idempotencyRepository.save(record);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeRequest(String key, int responseStatus, Object responseBody) {
        idempotencyRepository.findByIdempotencyKey(key.trim()).ifPresent(record -> {
            record.setStatus("COMPLETED");
            record.setResponseStatus(responseStatus);
            try {
                record.setResponseBody(objectMapper.writeValueAsString(responseBody));
            } catch (Exception e) {
                record.setResponseBody("{}");
            }
            idempotencyRepository.save(record);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failRequest(String key) {
        idempotencyRepository.findByIdempotencyKey(key.trim()).ifPresent(record -> {
            record.setStatus("FAILED");
            idempotencyRepository.save(record);
        });
    }
}
