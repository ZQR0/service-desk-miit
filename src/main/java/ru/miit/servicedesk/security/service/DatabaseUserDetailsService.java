package ru.miit.servicedesk.security.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.miit.servicedesk.users.UserMapper.UserMapper;
import ru.miit.servicedesk.users.UserRepository;
import ru.miit.servicedesk.users.UserEntity;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public DatabaseUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByLoginIgnoreCase(login.trim())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return UserMapper.toUserDetails(user);
    }
}
