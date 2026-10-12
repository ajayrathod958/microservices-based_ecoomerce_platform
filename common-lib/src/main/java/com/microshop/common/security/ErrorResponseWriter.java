package com.microshop.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microshop.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;

final class ErrorResponseWriter {

    private ErrorResponseWriter() {
    }

    static void write(ObjectMapper mapper, HttpServletRequest request, HttpServletResponse response,
                      HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getWriter(),
                ApiResponse.error(status.value(), message, request.getRequestURI(), null));
    }
}
