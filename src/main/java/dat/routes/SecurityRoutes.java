package dat.routes;
import com.fasterxml.jackson.databind.ObjectMapper;
import dat.controllers.impl.SecurityController;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.security.RouteRole;

import static io.javalin.apibuilder.ApiBuilder.*;

public class SecurityRoutes {
    private static ObjectMapper jsonMapper = new ObjectMapper();
    private static SecurityController securityController = SecurityController.getInstance();
    public static EndpointGroup getSecurityRoutes() {
        return ()->{
            path("/auth", ()->{
                get("/test", ctx->ctx.json(jsonMapper.createObjectNode().put("msg",  "Hello from Open")),Role.ANYONE);
                post("/login", securityController::login, Role.ANYONE);
                post("/register", securityController::register, Role.ANYONE);
                //get("/verify", securityController::verify, Role.ANYONE);
            });
        };
    }

    public static EndpointGroup getSecuredRoutes(){
        return ()->{
            path("/protected", ()->{
                get("/user_demo",(ctx)->ctx.json(jsonMapper.createObjectNode().put("msg",  "Hello from USER Protected")),Role.USER);
                get("/admin_demo",(ctx)->ctx.json(jsonMapper.createObjectNode().put("msg",  "Hello from ADMIN Protected")),Role.ADMIN);
            });
        };
    }
    public enum Role implements RouteRole { ANYONE, USER, ADMIN }
}