package heizoel.backend.adapter.in.web.error;

import heizoel.backend.adapter.in.web.security.InvalidApiKeyException;
import heizoel.backend.adapter.in.web.security.MissingApiKeyException;
import heizoel.backend.application.exception.*;
import heizoel.backend.application.port.out.dispo.DispoCallbackException;
import heizoel.backend.application.port.out.notification.NotificationDeliveryException;
import heizoel.backend.domain.exception.ConfirmationRequestExpiredException;
import heizoel.backend.domain.exception.ConfirmationRequestInactiveException;
import heizoel.backend.domain.exception.CustomerResponseAlreadyExistsException;
import heizoel.backend.domain.exception.InvalidDeliveryWindowException;
import heizoel.backend.domain.exception.MissingDigitalContactException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Clock;
import java.time.Instant;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final Clock clock;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponseDto> validation(MethodArgumentNotValidException e, HttpServletRequest req) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .orElse("The request contains invalid data.");

        return respond(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, req.getRequestURI());
    }

    @ExceptionHandler(InvalidDeliveryWindowException.class)
    ResponseEntity<ErrorResponseDto> invalidDeliveryWindow(InvalidDeliveryWindowException e, HttpServletRequest req) {
        return respond(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", e.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponseDto> malformedJson(HttpMessageNotReadableException e, HttpServletRequest req) {
        return respond(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request body is missing or malformed.", req.getRequestURI()
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErrorResponseDto> invalidRequestParameter(
            MethodArgumentTypeMismatchException e,
            HttpServletRequest req
    ) {
        return respond(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Request parameter has an invalid value.",
                req.getRequestURI()
        );
    }

    @ExceptionHandler(NotificationDeliveryException.class)
    ResponseEntity<ErrorResponseDto> notificationDeliveryFailed(
            NotificationDeliveryException e,
            HttpServletRequest req
    ) {
        String code = switch (e.getChannel()) {
            case EMAIL -> "EMAIL_SENDING_FAILED";
            case SMS -> "SMS_SENDING_FAILED";
            case WHATSAPP -> "WHATSAPP_SENDING_FAILED";
        };

        return respond(HttpStatus.BAD_GATEWAY, code, e.getMessage(), req.getRequestURI());
    }


    @ExceptionHandler(ConfirmationRequestNotFoundException.class)
    ResponseEntity<ErrorResponseDto> confirmationRequestNotFound(ConfirmationRequestNotFoundException e, HttpServletRequest req
    ) {
        return respond(HttpStatus.NOT_FOUND, "CONFIRMATION_REQUEST_NOT_FOUND", e.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(CompanyNotFoundException.class)
    ResponseEntity<ErrorResponseDto> companyNotFound(CompanyNotFoundException e, HttpServletRequest req) {
        return respond(HttpStatus.NOT_FOUND, "COMPANY_NOT_FOUND", e.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(OrderNotFoundException.class)
    ResponseEntity<ErrorResponseDto> orderNotFound(OrderNotFoundException e, HttpServletRequest req) {
        return respond(HttpStatus.NOT_FOUND, "ORDER_SNAPSHOT_NOT_FOUND", e.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(ConfirmationRequestInactiveException.class)
    ResponseEntity<ErrorResponseDto> confirmationRequestInactive(ConfirmationRequestInactiveException e, HttpServletRequest req
    ) {
        return respond(HttpStatus.CONFLICT, "CONFIRMATION_REQUEST_INACTIVE", e.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(ConfirmationRequestExpiredException.class)
    ResponseEntity<ErrorResponseDto> confirmationRequestExpired(
            ConfirmationRequestExpiredException e,
            HttpServletRequest req
    ) {
        return respond(HttpStatus.GONE, "CONFIRMATION_REQUEST_EXPIRED", e.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(CustomerResponseAlreadyExistsException.class)
    ResponseEntity<ErrorResponseDto> customerResponseAlreadyExists(CustomerResponseAlreadyExistsException e, HttpServletRequest req) {
        return respond(HttpStatus.CONFLICT, "CUSTOMER_RESPONSE_ALREADY_EXISTS", e.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorResponseDto> noResourceFound(NoResourceFoundException e, HttpServletRequest req) {
        return respond(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource was not found.", req.getRequestURI());
    }

    @ExceptionHandler(DispoCallbackException.class)
    ResponseEntity<ErrorResponseDto> dispoCallbackFailed(DispoCallbackException e, HttpServletRequest req
    ) {
       return respond(HttpStatus.BAD_GATEWAY, "DISPO_CALLBACK_FAILED", e.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponseDto> unexpected(Exception e, HttpServletRequest req) {
        log.error("Unexpected error occurred", e);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected technical error occurred.", req.getRequestURI());
    }

    @ExceptionHandler(MissingDigitalContactException.class)
    ResponseEntity<ErrorResponseDto> missingDigitalContact(MissingDigitalContactException e, HttpServletRequest req
    ) {
        return respond(HttpStatus.UNPROCESSABLE_ENTITY, "MISSING_DIGITAL_CONTACT", e.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(InvalidFilterException.class)
    ResponseEntity<ErrorResponseDto> invalidDashboardFilter(InvalidFilterException e, HttpServletRequest req) {
        return respond(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", e.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(MissingApiKeyException.class)
    ResponseEntity<ErrorResponseDto> missingApiKey(MissingApiKeyException e, HttpServletRequest req) {
        return respond(HttpStatus.UNAUTHORIZED, "MISSING_API_KEY", e.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(InvalidApiKeyException.class)
    ResponseEntity<ErrorResponseDto> invalidApiKey(InvalidApiKeyException e, HttpServletRequest req) {
        return respond(HttpStatus.UNAUTHORIZED, "INVALID_API_KEY", e.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(InvalidEmailSettingsException.class)
    ResponseEntity<ErrorResponseDto> invalidEmailSettings(InvalidEmailSettingsException e, HttpServletRequest req) {
        return respond(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", e.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(ConfirmationRequestDeliveryInProgressException.class)
    ResponseEntity<ErrorResponseDto> confirmationRequestDeliveryInProgress(
            ConfirmationRequestDeliveryInProgressException e,
            HttpServletRequest req
    ) {
        return respond(
                HttpStatus.CONFLICT,
                "CONFIRMATION_REQUEST_DELIVERY_IN_PROGRESS",
                e.getMessage(),
                req.getRequestURI()
        );
    }

    @ExceptionHandler(EmailSettingsNotConfiguredException.class)
    ResponseEntity<ErrorResponseDto> emailSettingsNotConfigured(
            EmailSettingsNotConfiguredException exception,
            HttpServletRequest request
    ) {
        return respond(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "EMAIL_SETTINGS_NOT_CONFIGURED",
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(TestEmailDeliveryException.class)
    ResponseEntity<ErrorResponseDto> testEmailDeliveryFailed(
            TestEmailDeliveryException exception,
            HttpServletRequest request
    ) {
        return respond(
                HttpStatus.BAD_GATEWAY,
                "SMTP_TEST_MESSAGE_FAILED",
                exception.getMessage(),
                request.getRequestURI()
        );
    }


    @ExceptionHandler(EmailConnectionTestException.class)
    ResponseEntity<ErrorResponseDto> emailConnectionTestFailed(
            EmailConnectionTestException exception,
            HttpServletRequest request
    ) {
        return respond(
                HttpStatus.BAD_GATEWAY,
                "SMTP_CONNECTION_FAILED",
                exception.getMessage(),
                request.getRequestURI()
        );
    }

    private ResponseEntity<ErrorResponseDto> respond(
            HttpStatus status,
            String code,
            String message,
            String path
    ) {
        return ResponseEntity
                .status(status)
                .body(new ErrorResponseDto(
                        code,
                        message,
                        status.value(),
                        path,
                        Instant.now(clock)
                ));
    }
}
