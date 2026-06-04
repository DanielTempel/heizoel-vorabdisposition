package heizoel.backend.location.infrastructure;

import heizoel.backend.customer.api.dto.CustomerConfirmationPreviewDto;
import heizoel.backend.location.application.interfaces.LocationTrackingService;
import heizoel.backend.location.domain.TrackingPreviewData;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice(annotations = Controller.class)
@RequiredArgsConstructor
public class CustomerConfirmationTrackingResponseAdvice implements ResponseBodyAdvice<Object> {

    private final LocationTrackingService locationTrackingService;

    @Override
    public boolean supports(
            MethodParameter returnType,
            Class<? extends HttpMessageConverter<?>> converterType
    ) {
        return CustomerConfirmationPreviewDto.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response
    ) {
        if (!(body instanceof CustomerConfirmationPreviewDto previewDto)) {
            return body;
        }

        HttpServletRequest servletRequest =
                ((ServletServerHttpRequest) request).getServletRequest();
        if (!"GET".equalsIgnoreCase(servletRequest.getMethod())
                || !servletRequest.getRequestURI().matches("^/api/customer/confirmations/[^/]+$")) {
            return body;
        }

        String token = servletRequest.getRequestURI()
                .substring(servletRequest.getRequestURI().lastIndexOf('/') + 1);

        Map<String, Object> responseBody = new LinkedHashMap<>();
        responseBody.put("externalOrderId", previewDto.externalOrderId());
        responseBody.put("customerName", previewDto.customerName());
        responseBody.put("deliveryAddress", previewDto.deliveryAddress());

        locationTrackingService.findTrackingPreviewByToken(token)
                .ifPresentOrElse(
                        tracking -> appendTrackingFields(responseBody, tracking),
                        () -> appendTrackingFields(responseBody, null)
                );

        responseBody.put("product", previewDto.product());
        responseBody.put("quantityLiters", previewDto.quantityLiters());
        responseBody.put("deliveryDate", previewDto.deliveryDate());
        responseBody.put("deliveryWindowStart", previewDto.deliveryWindowStart());
        responseBody.put("deliveryWindowEnd", previewDto.deliveryWindowEnd());
        responseBody.put("confirmationStatus", previewDto.confirmationStatus());
        return responseBody;
    }

    private void appendTrackingFields(Map<String, Object> responseBody, TrackingPreviewData tracking) {
        responseBody.put("locationX", tracking != null ? tracking.locationX() : null);
        responseBody.put("locationY", tracking != null ? tracking.locationY() : null);
        responseBody.put("targetLocationX", tracking != null ? tracking.targetLocationX() : null);
        responseBody.put("targetLocationY", tracking != null ? tracking.targetLocationY() : null);
    }
}
