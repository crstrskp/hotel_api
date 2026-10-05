package dat.entities;

import lombok.Getter;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class User {

    @Getter
    private String username;
    private Set<Role> roles = new HashSet<>();

    public User(String username) {
        this.username = Objects.requireNonNull(username);
    }

    public void addRole(Role role) {
        roles.add(Objects.requireNonNull(role));
    }

    public Set<String> getRolesAsStrings() {
        return roles.stream().map(Role::getRoleName).collect(Collectors.toSet());
    }
}
