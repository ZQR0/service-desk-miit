package rut.miit.servicedesk.requests;

import java.util.EnumSet;
import java.util.Set;

public enum RequestStatus {
    /** Создана преподавателем, ждёт согласования зав. кафедрой. */
    PENDING_HEAD("Ожидает согласования зав. кафедрой"),

    /** Согласована зав. кафедрой, ждёт подтверждения отделом и назначения исполнителя. */
    PENDING_DEPARTMENT("Ожидает подтверждения отделом"),

    /** Подтверждена отделом, назначен исполнитель (дежурный), работа ещё не начата. */
    ASSIGNED("Исполнитель назначен"),

    /** Дежурный выполняет работу. */
    IN_PROGRESS("В работе"),

    /** Выполнена и закрыта. */
    DONE("Выполнена"),

    /** Отклонена зав. кафедрой (обязательна причина). */
    REJECTED_BY_HEAD("Отклонена зав. кафедрой"),

    /** Отклонена отделом (обязательна причина). */
    REJECTED_BY_DEPARTMENT("Отклонена отделом"),

    /** Отменена автором до начала выполнения. */
    CANCELLED("Отменена");

    private final String title;

    RequestStatus(String title) {
        this.title = title;
    }

    /** Название статуса для отображения пользователю. */
    public String title() {
        return title;
    }

    /** Статусы, в которые заявку можно перевести из текущего. У конечных статусов набор пустой. */
    public Set<RequestStatus> nextStatuses() {
        return switch (this) {
            case PENDING_HEAD -> EnumSet.of(PENDING_DEPARTMENT, REJECTED_BY_HEAD, CANCELLED);
            case PENDING_DEPARTMENT -> EnumSet.of(ASSIGNED, REJECTED_BY_DEPARTMENT, CANCELLED);
            case ASSIGNED -> EnumSet.of(IN_PROGRESS, CANCELLED);
            case IN_PROGRESS -> EnumSet.of(DONE);
            case DONE, REJECTED_BY_HEAD, REJECTED_BY_DEPARTMENT, CANCELLED -> EnumSet.noneOf(RequestStatus.class);
        };
    }

    /** Допустим ли переход из текущего статуса в {@code target}. */
    public boolean canTransitionTo(RequestStatus target) {
        return nextStatuses().contains(target);
    }

    /** Конечный статус: заявка закрыта, дальше она не меняется. */
    public boolean isTerminal() {
        return nextStatuses().isEmpty();
    }

    /** Заявка ещё в процессе (учитывается в просрочках и «заявках в работе»). */
    public boolean isOpen() {
        return !isTerminal();
    }

    /** Для статуса обязательна причина отклонения ({@code request.reject_reason}). */
    public boolean requiresRejectReason() {
        return this == REJECTED_BY_HEAD || this == REJECTED_BY_DEPARTMENT;
    }

    /** Для статуса обязателен исполнитель ({@code request.executor_id}). */
    public boolean requiresExecutor() {
        return this == ASSIGNED  || this == IN_PROGRESS  || this == DONE;
    }

    /** Для статуса заполняется время закрытия ({@code request.completed_at}); только у DONE. */
    public boolean requiresCompletedAt() {
        return this == DONE;
    }
}
