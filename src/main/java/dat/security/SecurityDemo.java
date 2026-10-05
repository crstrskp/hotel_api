package dat.security;

import dat.config.HibernateConfig;
import dat.entities.Role;
import dat.entities.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

/** Run with the same local PostgreSQL setup as Hotel API. */
public class SecurityDemo {

    public static void main(String[] args) {
        // The current development configuration recreates the schema at startup.
        EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory("hotel");
        try {
            try (EntityManager em = emf.createEntityManager()) {
                em.getTransaction().begin();
                try {
                    Role role = new Role("user");
                    User user = new User("alice", "classroom-password");
                    user.addRole(role);
                    em.persist(role);
                    em.persist(user);
                    em.getTransaction().commit();
                } catch (RuntimeException exception) {
                    if (em.getTransaction().isActive()) em.getTransaction().rollback();
                    throw exception;
                }
            }

            // A new entity manager reloads the user from the database.
            try (EntityManager em = emf.createEntityManager()) {
                User user = em.find(User.class, "alice");
                System.out.println("Username: " + user.getUsername());
                System.out.println("Roles: " + user.getRolesAsStrings());
                System.out.println("Correct password: " + user.verifyPassword("classroom-password"));
                System.out.println("Incorrect password: " + user.verifyPassword("wrong"));
            }
        } finally {
            emf.close();
        }
    }
}
