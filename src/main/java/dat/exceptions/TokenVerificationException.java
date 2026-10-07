package dat.exceptions;

import com.auth0.jwt.exceptions.JWTVerificationException;

public class TokenVerificationException extends JWTVerificationException {
    public TokenVerificationException(String message) {
        super(message);
    }

    public TokenVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
