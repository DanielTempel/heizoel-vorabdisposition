package heizoel.backend.adapter.in.web.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import heizoel.backend.adapter.in.web.error.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationErrorHandler
        implements AuthenticationEntryPoint, AuthenticationFailureHandler {

    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        writeError(
                request,
                response,
                "MISSING_API_KEY",
                "Missing API key."
        );
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {
        writeError(
                request,
                response,
                "INVALID_API_KEY",
                "Invalid API key."
        );
    }

    private void writeError(
            HttpServletRequest request,
            HttpServletResponse response,
            String code,
            String message
    ) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponseDto errorResponse = new ErrorResponseDto(
                code,
                message,
                HttpServletResponse.SC_UNAUTHORIZED,
                request.getRequestURI(),
                Instant.now(clock)
        );

        objectMapper.writeValue(
                response.getOutputStream(),
                errorResponse
        );
    }

}