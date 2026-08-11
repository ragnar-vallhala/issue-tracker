package com.its.user.seed;

import com.its.user.entity.Role;
import com.its.user.repository.UserRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Loads the development dataset on an empty table (SRS 10.5).
 *
 * <p>A {@code CommandLineRunner} rather than a {@code data.sql}, for one reason: the
 * reference workbook lists passwords in plaintext, and they must be BCrypt hashes in the
 * database. Hashing needs the encoder, so the seed has to run through Java. Committing
 * pre-computed hashes to a SQL file would work but would obscure which password each one
 * corresponds to, which matters when someone needs to log in as a particular person to
 * look at their dashboard.
 *
 * <p>Users 101-104 are the reference workbook's own rows, unchanged. The rest extend the
 * cast so that the dashboards, filters and assignee pickers have enough people to be
 * worth looking at - three Project Owners with distinct portfolios, and eight Assignees
 * across different disciplines.
 *
 * <p>Ids are assigned explicitly to match the workbook, and the AUTO_INCREMENT counter is
 * then advanced past them so generated ids continue the same sequence rather than
 * colliding with it.
 */
@Component
@ConditionalOnProperty(name = "its.seed.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final String INSERT = """
            INSERT INTO user (user_id, name, email, password, profile, role)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

    /**
     * Every seeded account uses this password, apart from the four workbook users who
     * keep theirs. One shared value keeps the README's account table short enough that
     * people actually read it.
     */
    private static final String DEFAULT_PASSWORD = "Password!2026";

    private record SeedUser(int id, String name, String email, String password,
                            String profile, Role role) {
    }

    private static final List<SeedUser> USERS = List.of(
            // --- the reference workbook's four rows, verbatim ------------------------
            new SeedUser(101, "Emily Sinha", "emily.sinha@example.com", "EmilySecure!2025",
                    "Project owner with a focus on platform delivery", Role.PROJECT_OWNER),
            new SeedUser(102, "Michael Patel", "michael.patel@example.com", "MichaelPass#2025",
                    "Software engineer working across the notifications stack", Role.ASSIGNEE),
            new SeedUser(103, "Priya Jackson", "priya.jackson@example.com", "PriyaSafe@2025",
                    "Seasoned project owner leading analytics initiatives", Role.PROJECT_OWNER),
            new SeedUser(104, "Carlos Singh", "carlos.singh@example.com", "CarlosStrong$2025",
                    "Front-end developer focused on accessibility", Role.ASSIGNEE),

            // --- additional cast, so the dashboards have something to show -----------
            new SeedUser(105, "Aisha Rahman", "aisha.rahman@example.com", DEFAULT_PASSWORD,
                    "Backend engineer, payments and billing systems", Role.ASSIGNEE),
            new SeedUser(106, "Tom Okafor", "tom.okafor@example.com", DEFAULT_PASSWORD,
                    "QA engineer with a weakness for edge cases", Role.ASSIGNEE),
            new SeedUser(107, "Lena Fischer", "lena.fischer@example.com", DEFAULT_PASSWORD,
                    "Project owner for mobile and design systems", Role.PROJECT_OWNER),
            new SeedUser(108, "Diego Morales", "diego.morales@example.com", DEFAULT_PASSWORD,
                    "Mobile engineer, iOS and Android", Role.ASSIGNEE),
            new SeedUser(109, "Sofia Bergman", "sofia.bergman@example.com", DEFAULT_PASSWORD,
                    "Design engineer bridging Figma and production CSS", Role.ASSIGNEE),
            new SeedUser(110, "Raj Mehta", "raj.mehta@example.com", DEFAULT_PASSWORD,
                    "Platform and infrastructure, keeps the pipelines honest", Role.ASSIGNEE),
            new SeedUser(111, "Chloe Dubois", "chloe.dubois@example.com", DEFAULT_PASSWORD,
                    "Data engineer working on search and relevance", Role.ASSIGNEE),
            new SeedUser(112, "Noah Adeyemi", "noah.adeyemi@example.com", DEFAULT_PASSWORD,
                    "Full-stack engineer, recently joined the profile team", Role.ASSIGNEE));

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

        // Note the role values: Project Owners are 0 and Assignees are 1, taken from the
        // reference workbook's sample data (SRS A-04). The profile column holds a
        // description of the person, not an image path (SRS A-16).
        for (SeedUser user : USERS) {
            jdbcTemplate.update(INSERT, user.id(), user.name(), user.email(),
                    passwordEncoder.encode(user.password()), user.profile(),
                    user.role().getCode());
        }

        int nextId = USERS.get(USERS.size() - 1).id() + 1;
        jdbcTemplate.execute("ALTER TABLE user AUTO_INCREMENT = " + nextId);

        log.info("Seeded {} users (ids 101-{}); next id is {}",
                USERS.size(), nextId - 1, nextId);
    }
}
