package heizoel.backend.location.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import heizoel.backend.location.application.dto.IncomingTrackingPayload;
import heizoel.backend.location.application.interfaces.LocationTrackingService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class ConfirmationRequestTrackingFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private final LocationTrackingService locationTrackingService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"/api/dispo/confirmation-requests".equals(request.getRequestURI())
                || !"POST".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
            captureTrackingPayload(wrappedRequest, wrappedResponse);
        } finally {
            wrappedResponse.copyBodyToResponse();
        }
    }

    private void captureTrackingPayload(
            ContentCachingRequestWrapper request,
            ContentCachingResponseWrapper response
    ) throws IOException {
        if (response.getStatus() != HttpServletResponse.SC_CREATED
                && response.getStatus() != HttpServletResponse.SC_OK) {
            return;
        }

        if (request.getContentType() == null
                || !request.getContentType().startsWith(MediaType.APPLICATION_JSON_VALUE)) {
            return;
        }

        byte[] content = request.getContentAsByteArray();
        if (content.length == 0) {
            return;
        }

        JsonNode payload = objectMapper.readTree(new String(content, StandardCharsets.UTF_8));
        if (!hasTrackingFields(payload)) {
            return;
        }

        locationTrackingService.captureConfirmationRequest(new IncomingTrackingPayload(
                payload.path("externalOrderId").asText(),
                payload.path("deliveryAddress").asText(),
                payload.path("locationX").asDouble(),
                payload.path("locationY").asDouble(),
                payload.path("targetLocationX").asDouble(),
                payload.path("targetLocationY").asDouble()
        ));
    }

    private boolean hasTrackingFields(JsonNode payload) {
        return payload.hasNonNull("externalOrderId")
                && payload.hasNonNull("deliveryAddress")
                && payload.hasNonNull("locationX")
                && payload.hasNonNull("locationY")
                && payload.hasNonNull("targetLocationX")
                && payload.hasNonNull("targetLocationY");
    }
}
