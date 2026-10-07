package dat.exceptions;

public class NotAuthorizedException extends ApiException {
    public NotAuthorizedException(int statusCode, String message) {
        super(statusCode, message);
    }
}
