package ru.miit.servicedesk.users.controller;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import ru.miit.servicedesk.users.DTO.ChangePasswordRequest;
import ru.miit.servicedesk.users.DTO.CreateUserRequest;
import ru.miit.servicedesk.users.DTO.UpdateUserRequest;
import ru.miit.servicedesk.users.DepartmentService;
import ru.miit.servicedesk.users.RoleRepository;
import ru.miit.servicedesk.users.UserRepository;
import ru.miit.servicedesk.users.UserService;

@Controller
@RequestMapping
public class UserMvcController {
    private final UserService userService;
    private final DepartmentService departmentService;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public UserMvcController(
            UserService userService,
            DepartmentService departmentService,
            RoleRepository roleRepository,
            UserRepository userRepository
    ) {
        this.userService = userService;
        this.departmentService = departmentService;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public String listUsers(Pageable pageable, Model model) {
        model.addAttribute("users", userService.findAll(pageable));
        return "users/list";
    }

    @GetMapping("/users/new")
    @PreAuthorize("hasRole('ADMIN')")
    public String newUser(Model model) {
        model.addAttribute("form", new CreateUserRequest("", "", "", "", null, null, null));
        addReferenceData(model);
        return "users/create";
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public String createUser(
            @Valid @ModelAttribute("form") CreateUserRequest form,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            addReferenceData(model);
            return "users/create";
        }
        try {
            userService.create(form);
            return "redirect:/users";
        } catch (RuntimeException ex) {
            bindingResult.reject("user.create.failed", ex.getMessage());
            addReferenceData(model);
            return "users/create";
        }
    }

    @GetMapping("/users/{id}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String editUser(@PathVariable Long id, Model model) {
        var user = userService.getById(id);
        model.addAttribute("userId", id);
        model.addAttribute("form", new UpdateUserRequest(
                user.name(), user.lastName(), user.patronymic(), user.roleId(), user.departmentId()));
        addReferenceData(model);
        return "users/edit";
    }

    @PostMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateUser(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") UpdateUserRequest form,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("userId", id);
            addReferenceData(model);
            return "users/edit";
        }
        userService.update(id, form);
        return "redirect:/users";
    }

    @GetMapping("/departments/{id}/head")
    @PreAuthorize("hasRole('ADMIN')")
    public String assignHeadForm(@PathVariable Long id, Model model) {
        model.addAttribute("departmentId", id);
        var currentHead = departmentService.getDepartmentHead(id);
        model.addAttribute("currentHead", currentHead);
        model.addAttribute("candidates", userRepository.findByDepartment_IdAndRole_NameRole(
                id, ru.miit.servicedesk.users.RoleName.DEPARTMENT_HEAD.name()));
        model.addAttribute("selectedUserId", currentHead == null ? null : currentHead.id());
        return "users/assign-head";
    }

    @PostMapping("/departments/{id}/head")
    @PreAuthorize("hasRole('ADMIN')")
    public String assignHead(@PathVariable Long id, @RequestParam Long userId) {
        departmentService.assignHead(id, userId);
        return "redirect:/users";
    }

    @GetMapping("/profile")
    public String profile(Authentication authentication, Model model) {
        Long userId = resolveUserId(authentication);
        model.addAttribute("user", userService.getById(userId));
        model.addAttribute("passwordForm", new ChangePasswordRequest(""));
        return "users/profile";
    }

    @PostMapping("/profile/password")
    public String changeOwnPassword(
            Authentication authentication,
            @Valid @ModelAttribute("passwordForm") ChangePasswordRequest passwordForm,
            BindingResult bindingResult,
            Model model
    ) {
        Long userId = resolveUserId(authentication);
        if (bindingResult.hasErrors()) {
            model.addAttribute("user", userService.getById(userId));
            return "users/profile";
        }
        userService.changePassword(userId, passwordForm);
        model.addAttribute("user", userService.getById(userId));
        model.addAttribute("passwordForm", new ChangePasswordRequest(""));
        model.addAttribute("passwordChanged", true);
        return "users/profile";
    }

    private void addReferenceData(Model model) {
        model.addAttribute("roles", roleRepository.findAll());
        model.addAttribute("departments", departmentService.listAll());
    }

    private Long resolveUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails details) {
            return userRepository.findByLoginIgnoreCase(details.getUsername())
                    .map(u -> u.getId())
                    .orElse(1L);
        }
        return 1L;
    }
}
