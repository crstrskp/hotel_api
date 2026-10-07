package dat.controllers.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dat.config.HibernateConfig;
import dat.daos.SecurityDAO;
import dat.dtos.UserDTO;
import dat.exceptions.ApiException;
import dat.exceptions.NotAuthorizedException;
import dat.exceptions.TokenVerificationException;
import dat.security.TokenSecurity;
import dat.utils.Utils;

import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.HttpStatus;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.validation.ValidationException;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Set;
import java.util.stream.Collectors;

public class SecurityController {

    private static final Logger logger = LoggerFactory.getLogger(SecurityController.class);

    private static SecurityController instance;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SecurityDAO securityDAO = SecurityDAO.getInstance(HibernateConfig.getEntityManagerFactory("hotel"));

    private final TokenSecurity tokenSecurity = new TokenSecurity();
    
    public static SecurityController getInstance()
    {
        if (instance == null) {
            instance = new SecurityController();
        }
        return instance;
    }

    //#region login
    public void login(Context ctx) {
        ObjectNode returnObject = objectMapper.createObjectNode(); // for sending json messages back to the client
        try {
            UserDTO user = ctx.bodyAsClass(UserDTO.class);
            UserDTO verifiedUser = securityDAO.getVerifiedUser(user.getUsername(), user.getPassword());
            String token = createToken(verifiedUser);

            ctx.status(200).json(returnObject
                    .put("token", token)
                    .put("username", verifiedUser.getUsername()));

        } catch (EntityNotFoundException | ValidationException e) {
            ctx.status(401);
            System.out.println(e.getMessage());
            ctx.json(returnObject.put("msg", e.getMessage()));
        }
    }

    private String createToken(UserDTO user) {
        try {
            String ISSUER;
            String TOKEN_EXPIRE_TIME;
            String SECRET_KEY;

            if (System.getenv("DEPLOYED") != null) {
                ISSUER = System.getenv("ISSUER");
                TOKEN_EXPIRE_TIME = System.getenv("TOKEN_EXPIRE_TIME");
                SECRET_KEY = System.getenv("SECRET_KEY");
            } else {
                ISSUER = Utils.getPropertyValue("ISSUER", "config.properties");
                TOKEN_EXPIRE_TIME = Utils.getPropertyValue("TOKEN_EXPIRE_TIME", "config.properties");
                SECRET_KEY = Utils.getPropertyValue("SECRET_KEY", "config.properties");
            }
            return tokenSecurity.createToken(user, ISSUER, TOKEN_EXPIRE_TIME, SECRET_KEY);
        } catch (Exception e) {
            logger.error("Could not create token", e);
            throw new ApiException(500, "Could not create token");
        }
    }

    //#endregion

    //#region register
    public void register(Context ctx) // to get a user
    {
        UserDTO request = ctx.bodyAsClass(UserDTO.class);
        UserDTO created = securityDAO.createUser(
                request.getUsername(), request.getPassword()
        );
        ctx.status(201).json(created);
    }

    //#region authentication
    public void authenticate(Context ctx) {
        // This is a preflight request => no need for authentication
        if (ctx.method().toString().equals("OPTIONS")) {
            ctx.status(200);
            return;
        }
        // If the endpoint is not protected with roles or is open to ANYONE role, then skip
        Set<String> allowedRoles = ctx.routeRoles().stream().map(role -> role.toString().toUpperCase()).collect(Collectors.toSet());
        if (isOpenEndpoint(allowedRoles))
            return;

        // If there is no token we do not allow entry
        UserDTO verifiedTokenUser = validateAndGetUserFromToken(ctx);
        ctx.attribute("user", verifiedTokenUser); // -> ctx.attribute("user") in ApplicationConfig beforeMatched filter
    }

    private boolean isOpenEndpoint(Set<String> allowedRoles) {
        // If the endpoint is not protected with any roles:
        if (allowedRoles.isEmpty())
            return true;

        // 1. Get permitted roles and Check if the endpoint is open to all with the ANYONE role
        if (allowedRoles.contains("ANYONE")) {
            return true;
        }
        return false;
    }

    private UserDTO validateAndGetUserFromToken(Context ctx) {
        String token = getToken(ctx);
        UserDTO verifiedTokenUser = verifyToken(token);
        if (verifiedTokenUser == null) {
            throw new UnauthorizedResponse("Invalid user or token"); // UnauthorizedResponse is javalin 6 specific but response is not json!
        }
        return verifiedTokenUser;
    }

    private static String getToken(Context ctx) {
        String header = ctx.header("Authorization");
        if (header == null) {
            throw new UnauthorizedResponse("Authorization header is missing"); // UnauthorizedResponse is javalin 6 specific but response is not json!
        }

        // If the Authorization Header was malformed, then no entry
        String token = header.split(" ")[1];
        if (token == null) {
            throw new UnauthorizedResponse("Authorization header is malformed"); // UnauthorizedResponse is javalin 6 specific but response is not json!
        }
        return token;
    }

    private UserDTO verifyToken(String token) {
        boolean IS_DEPLOYED = (System.getenv("DEPLOYED") != null);
        String SECRET = IS_DEPLOYED ? System.getenv("SECRET_KEY") : Utils.getPropertyValue("SECRET_KEY", "config.properties");

        try {
            if (tokenSecurity.tokenIsValid(token, SECRET) && tokenSecurity.tokenNotExpired(token)) {
                return tokenSecurity.getUserWithRolesFromToken(token);
            } else {
                throw new NotAuthorizedException(403, "Token is not valid");
            }
        } catch (NotAuthorizedException | TokenVerificationException e) {
            logger.error("Could not create token", e);
            throw new ApiException(HttpStatus.UNAUTHORIZED.getCode(), "Unauthorized. Could not verify token");
        }
    }
    //#endregion

    //#region authorization
    public void authorize(Context ctx) {
        Set<String> allowedRoles = ctx.routeRoles()
                .stream()
                .map(role -> role.toString().toUpperCase())
                .collect(Collectors.toSet());

        // 1. Check if the endpoint is open to all (either by not having any roles or having the ANYONE role set
        if (isOpenEndpoint(allowedRoles))
            return;
        // 2. Get user and ensure it is not null
        UserDTO user = ctx.attribute("user");
        if (user == null) {
            throw new ForbiddenResponse("No user was added from the token");
        }
        // 3. See if any role matches
        if (!userHasAllowedRole(user, allowedRoles))
            throw new ForbiddenResponse("User was not authorized with roles: " + user.getRoles() + ". Needed roles are: " + allowedRoles);
    }

    private static boolean userHasAllowedRole(UserDTO user, Set<String> allowedRoles) {
        return user.getRoles().stream()
                .anyMatch(role -> allowedRoles.contains(role.toUpperCase()));
    }

    //#endregion
}
