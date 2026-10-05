package dat.entities;

import lombok.Getter;

import java.util.Objects;

@Getter
public class Role {

    private String roleName;

    public Role(String roleName) {
        this.roleName = Objects.requireNonNull(roleName);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Role role)) return false;
        return Objects.equals(roleName, role.roleName);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(roleName);
    }
}
