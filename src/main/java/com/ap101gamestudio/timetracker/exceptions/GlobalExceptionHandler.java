package com.ap101gamestudio.timetracker.exceptions;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private final MessageSource messageSource;

  public GlobalExceptionHandler(MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  @ExceptionHandler(DomainException.class)
  public ResponseEntity<Map<String, String>> handleDomainException(DomainException ex) {
    String translatedMessage;
    try {
      translatedMessage = messageSource.getMessage(ex.getMessage(), null, LocaleContextHolder.getLocale());
    } catch (Exception e) {
      translatedMessage = ex.getMessage();
    }

    Map<String, String> response = new HashMap<>();
    response.put("error", translatedMessage);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }
}