package ru.miit.servicedesk.catalog.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ru.miit.servicedesk.catalog.DTO.CatalogItemForm;
import ru.miit.servicedesk.catalog.entity.ServiceCatalogEntity;
import ru.miit.servicedesk.catalog.service.CatalogService;

@Controller
@RequestMapping("/catalog")
public class ServiceCatalogController {

    private static final int PAGE_SIZE = 9;
    private final CatalogService catalogService;

    public ServiceCatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String q,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        Page<ServiceCatalogEntity> services = catalogService.findActive(
            q, PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by("nameService").ascending()));
        model.addAttribute("services", services);
        model.addAttribute("query", q);
        return "catalog/list";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model) {
        model.addAttribute("service", catalogService.getById(id));
        return "catalog/details";
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String manage(@RequestParam(defaultValue = "") String q,
                         @RequestParam(required = false) Boolean active,
                         @RequestParam(defaultValue = "0") int page,
                         Model model) {
        Page<ServiceCatalogEntity> services = catalogService.listAll(
            q, active, PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by("nameService").ascending()));
        model.addAttribute("services", services);
        model.addAttribute("query", q);
        model.addAttribute("activeFilter", active);
        return "catalog/manage";
    }

    @GetMapping("/admin/new")
    @PreAuthorize("hasRole('ADMIN')")
    public String createForm(Model model) {
        model.addAttribute("form", new CatalogItemForm());
        model.addAttribute("pageTitle", "Новая услуга");
        model.addAttribute("formAction", "/catalog/admin");
        return "catalog/form";
    }

    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String create(@Valid @ModelAttribute("form") CatalogItemForm form,
                         BindingResult result, Model model) {
        if (!result.hasErrors()) {
            try {
                ServiceCatalogEntity created = catalogService.create(
                    form.getNameService(), form.getDescription(), form.getDeadline());
                return "redirect:/catalog/" + created.getId();
            } catch (IllegalArgumentException exception) {
                result.reject("catalog.save.failed", exception.getMessage());
            }
        }
        model.addAttribute("pageTitle", "Новая услуга");
        model.addAttribute("formAction", "/catalog/admin");
        return "catalog/form";
    }

    @GetMapping("/admin/{id}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String editForm(@PathVariable Long id, Model model) {
        ServiceCatalogEntity service = catalogService.getById(id);
        CatalogItemForm form = new CatalogItemForm();
        form.setNameService(service.getNameService());
        form.setDescription(service.getDescription());
        form.setDeadline(service.getDeadline());
        model.addAttribute("form", form);
        model.addAttribute("pageTitle", "Редактирование услуги");
        model.addAttribute("formAction", "/catalog/admin/" + id);
        model.addAttribute("serviceId", id);
        return "catalog/form";
    }

    @PostMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") CatalogItemForm form,
                         BindingResult result, Model model) {
        if (!result.hasErrors()) {
            try {
                catalogService.update(id, form.getNameService(), form.getDescription(), form.getDeadline());
                return "redirect:/catalog/" + id;
            } catch (IllegalArgumentException exception) {
                result.reject("catalog.save.failed", exception.getMessage());
            }
        }
        model.addAttribute("pageTitle", "Редактирование услуги");
        model.addAttribute("formAction", "/catalog/admin/" + id);
        model.addAttribute("serviceId", id);
        return "catalog/form";
    }

    @PostMapping("/admin/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public String deactivate(@PathVariable Long id) {
        catalogService.deactivate(id);
        return "redirect:/catalog/admin";
    }
}
