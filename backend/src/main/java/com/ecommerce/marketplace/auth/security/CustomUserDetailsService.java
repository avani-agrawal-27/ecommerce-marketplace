package com.ecommerce.marketplace.auth.security;

import com.ecommerce.marketplace.user.entity.User;
import com.ecommerce.marketplace.user.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found"
                        )
                );

        return buildUserDetails(user);
    }

    public UserDetails loadUserByUserId(String userId)
            throws UsernameNotFoundException {

        UUID id;

        try {
            id = UUID.fromString(userId);
        } catch (IllegalArgumentException exception) {
            throw new UsernameNotFoundException("Invalid user ID");
        }

        User user = userRepository.findByIdWithRoles(id)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found")
                );

        return buildUserDetails(user);
    }
    private UserDetails buildUserDetails(User user) {

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .disabled(!user.isEnabled())
                .authorities(
                        user.getRoles()
                                .stream()
                                .map(role ->
                                        new SimpleGrantedAuthority(
                                                "ROLE_" + role.getName()
                                        )
                                )
                                .toList()
                )
                .build();
    }
}