package rut.miit.servicedesk.common;

/**
 * Исключение, выбрасываемое при возникновении конфликта состояния ресурса.
 * Например, при попытке совершить недопустимый переход статуса заявки.
 * Соответствует HTTP статусу 409 Conflict.
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
