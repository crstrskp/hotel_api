## Hotel REST Exercise

This is a simple REST API for a hotel booking system. 
It is implemented using Javalin and JPA

![Hotel](./docs/bates_hotel.jpg)

### How to run

1. Create a database in your local Postgres instance called `hotel`
2. Run the main method in the config.Populate class to populate the database with some data
3. Run the main method in the Main class to start the server on port 7070
4. See the routes in your browser at `http://localhost:7070/routes`
5. Request the `http://localhost:7070/hotels` endpoint in your browser to see the list of hotels and rooms
6. Use the dev.http file to test the routes, GET/POST/PUT/DELETE requests are available

### Exercises
[tuesday class exercise](./tuesday-security-exercise.md)

### API tests

With JDK 25 and Docker running, execute `mvn test` (or run `HotelRouteTest` in your IDE).
The tests start Javalin on an available port and use an isolated PostgreSQL database
through Testcontainers. Each test seeds its own data; the local `hotel` database is
not used, and you do not need to start the application yourself.

The three tests use REST Assured's Given/When/Then syntax and Hamcrest assertions
to check GET, POST, and DELETE, including whether changes persist. They deliberately
send no authentication token. When authentication is added to the write routes,
the POST and DELETE tests should fail until their requests are updated to log in
and send a bearer token. GET can continue to pass if reading hotels remains public.

