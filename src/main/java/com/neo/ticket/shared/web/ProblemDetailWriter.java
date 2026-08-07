package com.neo.ticket.shared.web;

import com.neo.ticket.shared.error.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class ProblemDetailWriter {

    private final ObjectMapper objectMapper;

    public ProblemDetailWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletResponse response, ErrorCode errorCode, String detail)
            throws IOException {
        write(response, errorCode, detail, Map.of());
    }

    public void write(HttpServletResponse response, ErrorCode errorCode, String detail,
                      Map<String, Object> properties) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        ProblemDetail problem = ProblemDetails.of(errorCode, detail, properties);
        response.setStatus(ProblemDetails.statusFor(errorCode).value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
