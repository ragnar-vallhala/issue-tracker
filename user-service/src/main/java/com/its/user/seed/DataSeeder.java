package com.its.user.seed;

import com.its.user.entity.Role;
import com.its.user.repository.UserRepository;
import java.util.ArrayList;
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
 * <p>The cast is built in three layers, and the distinction matters when reading a screen
 * that looks wrong:
 *
 * <ul>
 *   <li><b>101-104</b> are the reference workbook's own rows, unchanged.
 *   <li><b>105-112</b> extend the cast so the dashboards, filters and assignee pickers
 *       have enough people to be worth looking at.
 *   <li><b>113-120</b> are deliberate edge cases - a name in a non-Latin script, an
 *       apostrophe that breaks naive escaping, a profile at the column limit, an owner
 *       with no projects and an assignee with no issues. Each one is a fixture for a
 *       screen that renders correctly on tidy data and badly on real data.
 *   <li><b>121 and up</b> are generated, to give the system a population rather than a
 *       cast. See {@link #generatedUsers()}.
 * </ul>
 *
 * <p>Ids are assigned explicitly, and the AUTO_INCREMENT counter is then advanced past
 * them so generated ids continue the same sequence rather than colliding with it.
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

    /** First generated id. Everything below this is hand-written above. */
    static final int FIRST_GENERATED_ID = 121;

    /** How many users to generate on top of the hand-written cast. */
    static final int GENERATED_COUNT = 50;

    /**
     * One generated user in every {@code OWNER_EVERY} is a Project Owner. The Project
     * Service's seeder mirrors this arithmetic to pick owners for its generated projects,
     * so that every {@code project_owner_id} still resolves to a role-0 user (FR-PRJ-02).
     * Changing the rule here means changing it there.
     */
    static final int OWNER_EVERY = 7;

    record SeedUser(int id, String name, String email, String password,
                    String profile, Role role) {
    }

    private static final List<SeedUser> CURATED = List.of(
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
                    "Full-stack engineer, recently joined the profile team", Role.ASSIGNEE),

            // --- edge cases ----------------------------------------------------------
            // Latin diacritics: the schema is utf8mb4, but a connection negotiated as
            // latin1 turns these into mojibake, and this row is where that shows first.
            new SeedUser(113, "Zeynep Çağlayan", "zeynep.caglayan@example.com", DEFAULT_PASSWORD,
                    "Reliability engineer, incident response and postmortems", Role.ASSIGNEE),
            // A name outside the Latin script entirely. Sorting, column widths and the
            // avatar initials all behave differently here.
            new SeedUser(114, "陈美玲", "meiling.chen@example.com", DEFAULT_PASSWORD,
                    "Compiler and build tooling, based in Shanghai", Role.ASSIGNEE),
            // The apostrophe is the point: prepared statements handle it, string
            // concatenation does not, and an unescaped JSP renders it as &#39;.
            new SeedUser(115, "Siobhán O'Connor", "siobhan.oconnor@example.com", DEFAULT_PASSWORD,
                    "Security engineer; asks \"what happens if I send that twice?\"", Role.ASSIGNEE),
            // Name and profile close to the 255-character column limit, to find the
            // table cells and cards that assume a short value.
            new SeedUser(116, "Bartholomew Fitzwilliam Ashworth-Beaumont III",
                    "b.ashworth.beaumont@example.com", DEFAULT_PASSWORD,
                    "Principal engineer working across the platform, payments and search "
                            + "groups, currently splitting time between the ledger migration "
                            + "and the relevance rewrite, and acting as the review backstop "
                            + "for anything that touches money or ranking.", Role.ASSIGNEE),
            // No profile at all. The column is nullable and the UI has to say something
            // sensible rather than printing "null".
            new SeedUser(117, "Mina Haddad", "mina.haddad@example.com", DEFAULT_PASSWORD,
                    null, Role.ASSIGNEE),
            // Owns nothing: the fixture for the empty owner dashboard (FR-UI-12).
            new SeedUser(118, "Grace Nakamura", "grace.nakamura@example.com", DEFAULT_PASSWORD,
                    "Project owner, currently between projects", Role.PROJECT_OWNER),
            // Assigned nothing: the fixture for the empty assignee dashboard.
            new SeedUser(119, "Oscar Lindqvist", "oscar.lindqvist@example.com", DEFAULT_PASSWORD,
                    "Engineer on secondment; no issues assigned this quarter", Role.ASSIGNEE),
            // Owns the two archive projects that hold the bulk of the generated issues.
            // Keeping that volume under one owner is deliberate: the owner dashboard
            // fetches every issue of every project it owns, so this account is the one
            // that exercises volume and the others stay quick to load.
            new SeedUser(120, "Arun Balakrishnan", "arun.balakrishnan@example.com",
                    DEFAULT_PASSWORD,
                    "Engineering manager; owns the imported historical backlogs",
                    Role.PROJECT_OWNER));

    // Generated names are drawn as a first x last grid rather than at random, so every
    // pair is distinct and the email built from it is unique without a numeric suffix.
    private static final String[] FIRST_NAMES = {
            "Ana", "Ben", "Cleo", "Dmitri", "Esther", "Farid", "Greta", "Hugo",
            "Imani", "Jonas"};

    private static final String[] LAST_NAMES = {
            "Alvarez", "Bruno", "Castellano", "Delacroix", "Eriksen",
            "Fontaine", "Gallagher", "Halvorsen", "Ibarra", "Jovanovic"};

    private static final String[] DISCIPLINES = {
            "Backend engineer, services and APIs",
            "Front-end engineer, component work",
            "QA engineer, automation and exploratory testing",
            "Data engineer, pipelines and warehousing",
            "Site reliability engineer, on-call and tooling",
            "Mobile engineer, cross-platform",
            "Security engineer, application security reviews",
            "Project owner, delivery and planning"};

    /**
     * The population behind the cast. Filters, assignee pickers and the sortable columns
     * all behave differently against fifty people than against twelve, and nothing about
     * these rows needs to be memorable - they exist to be numerous.
     *
     * <p>Deterministic by construction: index arithmetic, no randomness, so two clean
     * checkouts seed byte-identical databases and a screenshot stays reproducible.
     */
    private static List<SeedUser> generatedUsers() {
        List<SeedUser> users = new ArrayList<>(GENERATED_COUNT);

        for (int i = 0; i < GENERATED_COUNT; i++) {
            String first = FIRST_NAMES[i % FIRST_NAMES.length];
            String last = LAST_NAMES[(i / FIRST_NAMES.length) % LAST_NAMES.length];
            Role role = i % OWNER_EVERY == 0 ? Role.PROJECT_OWNER : Role.ASSIGNEE;

            users.add(new SeedUser(
                    FIRST_GENERATED_ID + i,
                    first + " " + last,
                    (first + "." + last).toLowerCase() + "@example.com",
                    DEFAULT_PASSWORD,
                    DISCIPLINES[i % DISCIPLINES.length],
                    role));
        }
        return users;
    }

    static final List<SeedUser> USERS = buildUsers();

    private static List<SeedUser> buildUsers() {
        List<SeedUser> users = new ArrayList<>(CURATED);
        users.addAll(generatedUsers());
        return List.copyOf(users);
    }

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
        //
        // BCrypt is deliberately slow, and at this row count the hashing - not the
        // inserts - is what the start-up cost is. Every generated account shares one
        // password, so its hash is computed once and reused; only the hand-written rows
        // with their own passwords are hashed individually.
        String sharedHash = passwordEncoder.encode(DEFAULT_PASSWORD);

        jdbcTemplate.batchUpdate(INSERT, USERS.stream()
                .map(user -> new Object[]{
                        user.id(), user.name(), user.email(),
                        DEFAULT_PASSWORD.equals(user.password())
                                ? sharedHash
                                : passwordEncoder.encode(user.password()),
                        user.profile(), user.role().getCode()})
                .toList());

        int nextId = USERS.get(USERS.size() - 1).id() + 1;
        jdbcTemplate.execute("ALTER TABLE user AUTO_INCREMENT = " + nextId);

        long owners = USERS.stream().filter(u -> u.role() == Role.PROJECT_OWNER).count();
        log.info("Seeded {} users (ids 101-{}, {} owners); next id is {}",
                USERS.size(), nextId - 1, owners, nextId);
    }
}
