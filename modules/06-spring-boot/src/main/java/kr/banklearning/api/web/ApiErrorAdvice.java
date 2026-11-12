package kr.banklearning.api.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import kr.banklearning.api.dto.ApiEnvelope;
import kr.banklearning.api.service.BankErrorCode;
import kr.banklearning.api.service.BankProblem;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiErrorAdvice {
    @ExceptionHandler(BankProblem.class)
    public ResponseEntity<ApiEnvelope<Void>> business(
            BankProblem problem, HttpServletRequest request) {
        return error(problem.getErrorCode(), request);
    }

    @ExceptionHandler({
        MethodArgumentTypeMismatchException.class,
        MissingServletRequestParameterException.class,
        HandlerMethodValidationException.class,
        MethodArgumentNotValidException.class,
        ConstraintViolationException.class
    })
    public ResponseEntity<ApiEnvelope<Void>> invalid(HttpServletRequest request) {
        return error(BankErrorCode.INVALID_REQUEST, request);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiEnvelope<Void>> store(HttpServletRequest request) {
        return error(BankErrorCode.STORE_UNAVAILABLE, request);
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiEnvelope<Void>> missing(HttpServletRequest request) {
        return error(BankErrorCode.NOT_FOUND, request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiEnvelope<Void>> method(HttpServletRequest request) {
        return error(BankErrorCode.METHOD_NOT_ALLOWED, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiEnvelope<Void>> unexpected(HttpServletRequest request) {
        return error(BankErrorCode.INTERNAL_ERROR, request);
    }

    private static ResponseEntity<ApiEnvelope<Void>> error(
            BankErrorCode errorCode, HttpServletRequest request) {
        String message =
                switch (errorCode.getCode()) {
                    case "INVALID_REQUEST" -> "요청 값을 확인하세요.";
                    case "NOT_FOUND" -> "대상을 찾을 수 없습니다.";
                    case "FORBIDDEN" -> "요청 권한이 없습니다.";
                    case "STORE_UNAVAILABLE" -> "데이터 저장소를 이용할 수 없습니다.";
                    default -> "요청을 처리하지 못했습니다.";
                };
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiEnvelope.failure(errorCode.getCode(), message, RequestTraceFilter.id(request)));
    }
}
