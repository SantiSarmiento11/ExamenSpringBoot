package tech.com.project.security.services;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import tech.com.project.model.Permission;
import tech.com.project.model.Role;
import tech.com.project.model.RolePermission;
import tech.com.project.model.UserAccount;
import tech.com.project.model.UserRole;

import java.util.*;

@Getter
@AllArgsConstructor
@Builder
public class UserDetailsImpl implements UserDetails {

    private final Integer id;
    private final String email;
    private final String firstName;
    private final String lastName;

    @JsonIgnore
    private final String password;

    private final boolean active;
    private final List<String> roleNames;
    private final List<String> permissionNames;
    private final Collection<? extends GrantedAuthority> authorities;

    public static UserDetailsImpl build(UserAccount user) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        List<String> roleNames = new ArrayList<>();
        Set<String> permissionNames = new HashSet<>();

        if (user.getUserRoles() != null) {
            for (UserRole userRole : user.getUserRoles()) {
                Role role = userRole.getRole();
                if (role != null) {
                    String roleName = role.getName();
                    roleNames.add(roleName);
                    // Rol con prefijo Spring Security
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));

                    if (role.getRolePermissions() != null) {
                        for (RolePermission rp : role.getRolePermissions()) {
                            Permission permission = rp.getPermission();
                            if (permission != null) {
                                permissionNames.add(permission.getName());
                                authorities.add(new SimpleGrantedAuthority(permission.getName()));
                            }
                        }
                    }
                }
            }
        }

        return UserDetailsImpl.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .password(user.getPasswordHash())
                .active(user.getIsActive() != null ? user.getIsActive() : true)
                .roleNames(roleNames)
                .permissionNames(new ArrayList<>(permissionNames))
                .authorities(authorities)
                .build();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
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
        return active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserDetailsImpl that = (UserDetailsImpl) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
