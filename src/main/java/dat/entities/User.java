package dat.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import org.mindrot.jbcrypt.BCrypt;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class User {

    @Getter
    private String username;
    @JsonIgnore
    private String passwordHash;
    private Set<Role> roles = new HashSet<>();

    public User(String username, String password) {
        this.username = Objects.requireNonNull(username);
        if (password == null || password.isBlank()
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Password must contain 1 to 72 UTF-8 bytes and not be blank");
        }
        // Store only the hash. bcrypt includes the generated salt in this string.
        this.passwordHash = BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    public boolean verifyPassword(String password) {
        return password != null
                && password.getBytes(StandardCharsets.UTF_8).length <= 72
                && BCrypt.checkpw(password, passwordHash);
    }

    public void addRole(Role role) {
        roles.add(Objects.requireNonNull(role));
    }

    public Set<String> getRolesAsStrings() {
        return roles.stream().map(Role::getRoleName).collect(Collectors.toSet());
    }
}
