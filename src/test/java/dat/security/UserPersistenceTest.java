package dat.security;

import dat.config.HibernateConfig;
import dat.entities.Role;
import dat.entities.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class UserPersistenceTest {

    private static EntityManagerFactory emf;

    @BeforeAll
    static void startDatabase() {
        emf = HibernateConfig.getEntityManagerFactoryForTest();
    }

    @AfterAll
    static void stopDatabase() {
        if (emf != null) emf.close();
        HibernateConfig.setTest(false);
    }

    @Test
    @DisplayName("Given a user with a role, when saved and reloaded, then persist the role and verify the hashed password")
    void userAndRolePersistAndPasswordCanBeVerified() {
        // Given a user with a password and a shared role.
        String password = "classroom-password";
        User alice = new User("alice", password);
        Role role = new Role("user");
        alice.addRole(role);

        // When both are saved in a transaction.
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            try {
                em.persist(role);
                em.persist(alice);
                em.getTransaction().commit();
            } catch (RuntimeException exception) {
                if (em.getTransaction().isActive()) em.getTransaction().rollback();
                throw exception;
            }
        }

        // Then a NEW entity manager can reload them from the database.
        User reloaded;
        try (EntityManager em = emf.createEntityManager()) {
            reloaded = em.find(User.class, "alice");
            assertThat(em.find(Role.class, "user"), notNullValue());

            // Inspect the actual stored value without exposing a hash getter on User.
            String storedHash = (String) em.createNativeQuery(
                            "SELECT password_hash FROM app_user WHERE username = :username")
                    .setParameter("username", "alice")
                    .getSingleResult();
            assertThat(storedHash, not(equalTo(password)));
            assertThat(storedHash, startsWith("$2a$12$"));
        }

        assertThat(reloaded, notNullValue());
        assertThat(reloaded.getUsername(), equalTo("alice"));
        assertThat(reloaded.getRolesAsStrings(), contains("user"));
        assertThat(reloaded.verifyPassword(password), is(true));
        assertThat(reloaded.verifyPassword("wrong"), is(false));
    }
}
