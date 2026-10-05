# Hotel API: users, roles, and passwords

Add users and roles to the Hotel API, store passwords with bcrypt, and write a
test that verifies the implementation.

Continue on your own working branch. The branches `step-1` to `step-4` contain
reference implementations; each includes the previous steps.

## 1. Add User and Role

Create `User` and `Role` in `dat.entities`.

1. Give Role a `String roleName`, a constructor, and a getter.
2. Give User a `String username`, a constructor, a getter, and a `Set<Role>`.
3. Implement `addRole(Role role)` and `getRolesAsStrings()` on User.
4. Implement Role's `equals()` and `hashCode()` using its name, so the set does
   not contain duplicate roles.

Test it: Create Alice with the role `user` and Bob with the roles `user` and `admin`.
Print their usernames and role names.


## 2. Hash and verify passwords

Add jbcrypt to pom.xml and reload Maven:

```xml
<dependency>
    <groupId>org.mindrot</groupId>
    <artifactId>jbcrypt</artifactId>
    <version>0.4</version>
</dependency>
```

1. Add a `passwordHash` field to User. Keep it out of JSON with `@JsonIgnore`.
2. Change the constructor to accept username and password. Store only the hash:
   `BCrypt.hashpw(password, BCrypt.gensalt(12))`.
3. Reject null, blank, or longer-than-72-UTF-8-byte passwords in the constructor.
4. Add `verifyPassword(String password)` using `BCrypt.checkpw`. Return false
   for null or oversized candidates.
5. Verify a correct password and an incorrect password. Expect true and false.

Import BCrypt from `org.mindrot.jbcrypt`.

Why would hashing the same password with a new salt and comparing the two hashes
fail to verify it?


## 3. Persist users and roles

1. Make both classes JPA entities with protected no-argument constructors.
2. Map User to `app_user`, using username as its primary key. Map passwordHash
   to `password_hash` with length 60.
3. Map Role to `app_role`, using roleName as its primary key.
4. Map User's roles with `@ManyToMany(fetch = FetchType.EAGER)` and a join table
   named `user_role`, with columns `username` and `role_name`.
5. Register both classes in `HibernateConfig.getAnnotationConfiguration`.
6. Create `dat.security.SecurityDemo` with a main method. Persist a role and a
   user in one transaction, then reload the user through a new entity manager.
7. Print the reloaded username, roles, and results of correct/incorrect password
   verification. Inspect the three tables in PostgreSQL.

Use the local classroom database with the Hotel API server stopped. The current
Hibernate configuration recreates the schema at startup, so save and reload in
the same program run. Persist the role explicitly before the user.


## 4. Write one integration test

Create `UserPersistenceTest` in `src/test/java/dat/security` using JUnit and Hamcrest.

1. Obtain the test factory with `HibernateConfig.getEntityManagerFactoryForTest()`.
2. **Given:** create Alice with a password and the role `user`.
3. **When:** persist the role and user, commit, and close the entity manager.
4. **Then:** reload through a new entity manager and assert that the username and
   role were saved, the database stores a hash rather than plaintext, the correct
   password verifies, and an incorrect password does not.
5. Close the factory after the test class and reset `HibernateConfig.setTest(false)`.

With Docker running, execute:

```bash
mvn -Dtest=UserPersistenceTest test
mvn test
```

The new test and the three existing Hotel API tests should pass.

