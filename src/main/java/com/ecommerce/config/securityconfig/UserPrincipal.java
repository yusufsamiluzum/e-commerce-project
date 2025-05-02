package com.ecommerce.config.securityconfig;

import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.ecommerce.entities.user.User;

public class UserPrincipal implements UserDetails {
    
    private User user;

    public UserPrincipal(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (user == null || user.getRoleType() == null) {
            return Collections.emptyList();
        }
        String role = "ROLE_" + user.getRoleType(); // Add prefix here
        return Collections.singletonList(new SimpleGrantedAuthority(role));
    }

    public Long getId() { // Use the correct return type (e.g., Long) matching User.getId()
        return this.user.getUserId(); // Assumes user is not null and User has getId()
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
