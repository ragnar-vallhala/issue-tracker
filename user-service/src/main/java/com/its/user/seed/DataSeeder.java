package com.its.user.seed;

import com.its.user.entity.Role;
import com.its.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Loads the reference workbook's sample users on an empty table (SRS 10.5).
 *
 * <p>A {@code CommandLineRunner} rather than a {@code data.sql}, for one reason: the
 * workbook lists passwords in plaintext, and they must be BCrypt hashes in the database.
 * Hashing needs the encoder, so the seed has to run through Java. Committing
 * pre-computed hashes to a SQL file would work but would obscure which password each
 * one corresponds to, which matters when someone needs to actually log in as Emily to
 * test the owner dashboard.
 *
 * <p>Ids are assigned explicitly (101-104) to match the workbook, and the AUTO_INCREMENT
 * counter is then advanced past them so generated ids continue the same sequence rather
 * than colliding with it.
 */
@Component
@ConditionalOnProperty(name = "its.seed.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final String INSERT = """
            INSERT INTO user (user_id, name, email, password, profile, role)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      JdbcTemplate jdbcTemplate,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.debug("Users already present - skipping seed");
            return;
        }

        // The four users from the reference workbook. Note the role values: the two
        // project owners are 0 and the two engineers are 1 (SRS A-04). The workbook's
        // profile column holds descriptions of the person, not image paths (SRS A-16);
        // the text is reconstructed from the truncated cells.
        seed(101, "Emily Sinha", "emily.sinha@example.com", "EmilySecure!2025",
                "Project owner with a focus on platform delivery", Role.PROJECT_OWNER);
        seed(102, "Michael Patel", "michael.patel@example.com", "MichaelPass#2025",
                "Software engineer working across the notifications stack", Role.ASSIGNEE);
        seed(103, "Priya Jackson", "priya.jackson@example.com", "PriyaSafe@2025",
                "Seasoned project owner leading analytics initiatives", Role.PROJECT_OWNER);
        seed(104, "Carlos Singh", "carlos.singh@example.com", "CarlosStrong$2025",
                "Front-end developer focused on accessibility", Role.ASSIGNEE);

        // Continue the workbook's numbering for anything created from here on.
        jdbcTemplate.execute("ALTER TABLE user AUTO_INCREMENT = 105");

        log.info("Seeded 4 reference users (ids 101-104); next id is 105");
    }

    private void seed(int id, String name, String email, String rawPassword,
                      String profile, Role role) {

        jdbcTemplate.update(INSERT, id, name, email,
                passwordEncoder.encode(rawPassword), profile, role.getCode());
    }
}
