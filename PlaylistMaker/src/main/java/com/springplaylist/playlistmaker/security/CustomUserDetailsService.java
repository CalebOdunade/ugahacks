package com.springplaylist.playlistmaker.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) {
        // Custom logic to load user data, for example, from a database
        if ("user".equals(username)) {
            return User.withUsername("user")
                    .password("$2a$10$7dCZVjVtMmTUBwfaV5k21qjHv.EZ.CmDgdVRuJ4fruZftBQFZ9W2u")  // Encrypted password (BCrypt)
                    .roles("USER")
                    .build();
        } else {
            throw new UsernameNotFoundException("User not found");
        }
    }
}
