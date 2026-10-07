package dat.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;

public record UserDTO(
        String username,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String password,
        Set<String> roles
) {
    public UserDTO(String username, Set<String> roles) {
        this(username, null, roles);
    }

    public Set<String> getRoles() {
        return roles;
    }

    public String getUsername()
    {
        return this.username;
    }

    public String getPassword()
    {
        return this.password;
    }
}
