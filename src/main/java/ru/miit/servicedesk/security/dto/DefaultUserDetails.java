package ru.miit.servicedesk.security.dto;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class DefaultUserDetails implements UserDetails {

    private String login;
    private String passwordHash;
    private String role;

    public DefaultUserDetails(String login, String passwordHash, String role) {
        this.login = login;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return List.of(new SimpleGrantedAuthority(authority));
    }

    @Override
    public String getPassword() {
        return this.passwordHash;
    }

    @Override
    public String getUsername() {
        return this.login;
    }
}
