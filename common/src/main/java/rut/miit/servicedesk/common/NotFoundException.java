package rut.miit.servicedesk.common;

/**
 * Исключение, выбрасываемое, когда запрашиваемый ресурс не найден.
 * Соответствует HTTP статусу 404 Not Found.
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
