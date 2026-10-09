package rut.miit.servicedesk.common;

import java.time.Instant;

/**
 * Стандартизированный формат ответа при возникновении ошибки в API.
 *
 * @param code      Код ошибки (например, "USER_NOT_FOUND")
 * @param message   Человекочитаемое описание ошибки
 * @param details   Дополнительные детали (например, список ошибок валидации полей)
 * @param timestamp Время возникновения ошибки в формате UTC
 */
public record ErrorResponse(
    String code,
    String message,
    Object details,
    Instant timestamp
) {}
