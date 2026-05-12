package heizoel.backend.dispo.infrastructure.error;

import heizoel.backend.exceptions.EmailSendingException;
import heizoel.backend.exceptions.customer.ConfirmationRequestExpiredException;
import heizoel.backend.exceptions.customer.ConfirmationRequestInactiveException;
import heizoel.backend.exceptions.customer.ConfirmationRequestNotFoundException;
import heizoel.backend.exceptions.customer.CustomerResponseAlreadyExistsException;
import heizoel.backend.exceptions.dispo.InvalidDeliveryWindowException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mail.MailException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

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

    @ExceptionHandler(EmailSendingException.class)
    ResponseEntity<ErrorResponseDto> emailSendingFailed(EmailSendingException e, HttpServletRequest req
    ) {
        return respond(HttpStatus.BAD_GATEWAY, "EMAIL_SENDING_FAILED", e.getMessage(), req.getRequestURI());
    }


    @ExceptionHandler(ConfirmationRequestNotFoundException.class)
    ResponseEntity<ErrorResponseDto> confirmationRequestNotFound(ConfirmationRequestNotFoundException e, HttpServletRequest req
    ) {
        return respond(HttpStatus.NOT_FOUND, "CONFIRMATION_REQUEST_NOT_FOUND", e.getMessage(), req.getRequestURI());
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

    @ExceptionHandler(MailException.class)
    ResponseEntity<ErrorResponseDto> handleMailException(MailException ex, HttpServletRequest request) {
        return respond(HttpStatus.BAD_GATEWAY, "EMAIL_SENDING_FAILED", "The confirmation e-mail could not be sent.", request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponseDto> unexpected(Exception e, HttpServletRequest req) {
        log.error("Unexpected error occurred", e);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected technical error occurred.", req.getRequestURI());
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
                        Instant.now()
                ));
    }
}