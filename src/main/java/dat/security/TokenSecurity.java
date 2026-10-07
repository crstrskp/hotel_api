package dat.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import dat.dtos.UserDTO;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/** JWT helper used by SecurityController. Expiration settings are milliseconds. */
public class TokenSecurity {

    public String createToken(UserDTO user, String issuer, String expirationTime, String secret) {
        long expirationMillis = Long.parseLong(expirationTime);
        if (expirationMillis <= 0 || issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("Issuer and positive expiration are required");
        }
        Instant now = Instant.now();
        return JWT.create()
                .withIssuer(issuer)
                .withSubject(user.getUsername())
                .withArrayClaim("roles", user.getRoles().toArray(String[]::new))
                .withIssuedAt(now)
                .withExpiresAt(now.plusMillis(expirationMillis))
                .sign(signingAlgorithm(secret));
    }

    /** Verifies the signature, expiry, and required identity claims. */
    public boolean tokenIsValid(String token, String secret) {
        Algorithm algorithm = signingAlgorithm(secret);
        try {
            var jwt = JWT.require(algorithm)
                    .withClaimPresence("sub")
                    .withClaimPresence("roles")
                    .withClaimPresence("exp")
                    .build().verify(token);
            if (jwt.getExpiresAtAsInstant() == null) return false;
            getUserWithRolesFromToken(token);
            return true;
        } catch (JWTVerificationException e) {
            return false;
        }
    }

    /** Checks expiry only; this does not verify the signature. */
    public boolean tokenNotExpired(String token) {
        try {
            Instant expiration = JWT.decode(token).getExpiresAtAsInstant();
            return expiration != null && Instant.now().isBefore(expiration);
        } catch (JWTVerificationException e) {
            return false;
        }
    }

    /** Call only after tokenIsValid: decoding alone does not authenticate a user. */
    public UserDTO getUserWithRolesFromToken(String token) {
        var jwt = JWT.decode(token);
        String username = jwt.getSubject();
        List<String> roles = jwt.getClaim("roles").asList(String.class);
        if (username == null || username.isBlank() || roles == null
                || roles.stream().anyMatch(role -> role == null || role.isBlank())) {
            throw new JWTVerificationException("Token must contain a username and roles");
        }
        return new UserDTO(username, Set.copyOf(roles));
    }

    private Algorithm signingAlgorithm(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("SECRET_KEY must be configured");
        }
        return Algorithm.HMAC256(secret);
    }
}
