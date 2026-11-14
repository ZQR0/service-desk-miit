package ru.miit.servicedesk.catalog.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class CatalogItemForm {

    @NotBlank(message = "Укажите название услуги")
    @Size(max = 255, message = "Название не должно превышать 255 символов")
    private String nameService;

    @Size(max = 4000, message = "Описание не должно превышать 4000 символов")
    private String description;

    @NotNull(message = "Укажите нормативный срок")
    @Positive(message = "Срок должен быть положительным числом")
    private Integer deadline;

    public String getNameService() { return nameService; }
    public void setNameService(String nameService) { this.nameService = nameService; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getDeadline() { return deadline; }
    public void setDeadline(Integer deadline) { this.deadline = deadline; }
}
