package dat.routes;

import dat.config.ApplicationConfig;
import dat.config.HibernateConfig;
import dat.dtos.HotelDTO;
import dat.entities.Hotel;
import io.javalin.Javalin;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Baseline API tests: requests deliberately have no authentication header.
 * Once write routes require authentication, POST and DELETE should fail until
 * students extend the requests with a token obtained through login.
 */
class HotelRouteTest {

    private static EntityManagerFactory emf;
    private static Javalin app;
    private static RequestSpecification requestSpec;
    private Integer hotelId;

    @BeforeAll
    static void startServer() {
        // Select the existing Testcontainers database BEFORE controllers are created.
        // Controllers and fixtures must use the same EntityManagerFactory.
        HibernateConfig.setTest(true);
        emf = HibernateConfig.getEntityManagerFactory("hotel");
        app = ApplicationConfig.startServer(0); // Ask the OS for an available port.
        requestSpec = new RequestSpecBuilder()
                .setBaseUri("http://localhost")
                .setPort(app.port())
                .setBasePath("/api/v1")
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .build();
    }

    @BeforeEach
    void seedDatabase() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.createQuery("DELETE FROM Room").executeUpdate();
            em.createQuery("DELETE FROM Hotel").executeUpdate();
            Hotel hotel = new Hotel("Test Hotel", "Test Street 1", Hotel.HotelType.STANDARD);
            em.persist(hotel);
            em.getTransaction().commit();
            hotelId = hotel.getId();
        }
    }

    @AfterAll
    static void stopServer() {
        if (app != null) {
            ApplicationConfig.stopServer(app);
        }
        if (emf != null) {
            emf.close(); // create-drop removes the test schema.
        }
        HibernateConfig.setTest(false);
    }

    @Test
    @DisplayName("Given an existing hotel, when GET hotels is called, then return its data with 200")
    void getHotelsReturnsSeededHotel() {
        given()
                .spec(requestSpec)
                .log().ifValidationFails()
        .when()
                .get("/hotels")
        .then()
                .log().ifValidationFails()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("", hasSize(1))
                .body("[0].id", equalTo(hotelId))
                .body("[0].hotelName", equalTo("Test Hotel"))
                .body("[0].hotelAddress", equalTo("Test Street 1"))
                .body("[0].hotelType", equalTo("STANDARD"))
                .body("[0].rooms", empty());
    }

    @Test
    @DisplayName("Given valid hotel data, when POST hotels is called, then create a hotel with 201")
    void postHotelCreatesAndPersistsHotel() {
        HotelDTO newHotel = new HotelDTO("New Hotel", "New Street 2", Hotel.HotelType.LUXURY);

        int createdId = given()
                .spec(requestSpec)
                .body(newHotel)
                .log().ifValidationFails()
        .when()
                .post("/hotels")
        .then()
                .log().ifValidationFails()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("id", greaterThan(0))
                .body("hotelName", equalTo("New Hotel"))
                .body("hotelAddress", equalTo("New Street 2"))
                .body("hotelType", equalTo("LUXURY"))
                .body("rooms", empty())
                .extract().path("id");

        // A separate request proves the hotel was saved, rather than just echoed.
        given()
                .spec(requestSpec)
                .pathParam("id", createdId)
        .when()
                .get("/hotels/{id}")
        .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("id", equalTo(createdId))
                .body("hotelName", equalTo("New Hotel"));
    }

    @Test
    @DisplayName("Given an existing hotel, when DELETE hotel is called, then remove it with 204")
    void deleteHotelRemovesPersistedHotel() {
        given()
                .spec(requestSpec)
                .pathParam("id", hotelId)
                .log().ifValidationFails()
        .when()
                .delete("/hotels/{id}")
        .then()
                .log().ifValidationFails()
                .statusCode(204)
                .body(isEmptyString());

        // Verify the deletion through the API; no dependency on another test.
        given()
                .spec(requestSpec)
        .when()
                .get("/hotels")
        .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("", empty());
    }
}
