package com.microshop.common.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventParser {

    private final ObjectMapper mapper;

    /** @throws UnparseableEventException (never retried: a malformed message cannot fix itself) */
    public <T> T parse(String json, Class<T> type) {
        if (json == null || json.isBlank()) {
            throw new UnparseableEventException("Empty message for " + type.getSimpleName(), null);
        }
        try {
            return mapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new UnparseableEventException("Cannot parse " + type.getSimpleName() + ": " + e.getOriginalMessage(), e);
        }
    }
}
