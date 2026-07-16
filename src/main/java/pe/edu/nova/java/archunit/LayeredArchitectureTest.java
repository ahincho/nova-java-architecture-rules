package pe.edu.nova.java.archunit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.GeneralCodingRules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

/**
 * Abstract JUnit 5 test that enforces the canonical Layered (N-tier)
 * architecture produced by the Nova Platform templates.
 *
 * <p>To activate this test in a Layered application, subclass it with a
 * concrete {@link AnalyzeClasses} declaration:
 *
 * <pre>{@code
 * @AnalyzeClasses(
 *     packages = "com.acme.my",
 *     importOptions = ImportOption.DoNotIncludeTests.class)
 * class ArchitectureTest extends LayeredArchitectureTest { }
 * }</pre>
 *
 * <p>Each rule below maps to one of the rules described in the
 * documentation of the {@code nova-java-quarkus-template} Layered
 * variant. Subclasses may override the package roots via
 * {@link #basePackage()} if needed; defaults assume the layered
 * convention ({@code controller}, {@code service},
 * {@code repository}, {@code entity}, {@code dto}).
 *
 * <p>The class is deliberately package-private final-free: subclasses
 * inherit all rules at once and can layer extra constraints on top.
 *
 * @see <a href="https://www.archunit.org/">ArchUnit</a>
 */
public abstract class LayeredArchitectureTest {

    /** Package holding the {@code @Path} / {@code @RestController} classes. */
    protected static final String CONTROLLER_PACKAGE = "controller";

    /** Package holding the {@code @ApplicationScoped} service classes. */
    protected static final String SERVICE_PACKAGE = "service";

    /** Package holding the data-access classes (interface + impl). */
    protected static final String REPOSITORY_PACKAGE = "repository";

    /** Package holding the persistence entities / domain records. */
    protected static final String ENTITY_PACKAGE = "entity";

    /** Package holding the transport objects (request / response DTOs). */
    protected static final String DTO_PACKAGE = "dto";

    /** Cross-cutting package (shared exceptions, identifiers). */
    protected static final String SHARED_PACKAGE = "shared";

    /**
     * Returns the base package that contains all the application's
     * code. Defaults to the root package passed to
     * {@link AnalyzeClasses} on the concrete subclass; override if
     * your modules use a deeper common root.
     *
     * @return the base package root.
     */
    protected abstract String basePackage();

    private String layered(final String leaf) {
        return basePackage() + "." + leaf + "..";
    }

    // ------------------------------------------------------------------
    // Layer-to-layer dependency direction
    // ------------------------------------------------------------------

    /**
     * Controllers may depend on services, DTOs, entities and shared —
     * never on repositories (must always go through a service).
     */
    @ArchTest
    private final ArchRule controllers_must_not_depend_on_repositories =
            noClasses().that().resideInAPackage(layered(CONTROLLER_PACKAGE))
                    .should().dependOnClassesThat().resideInAPackage(
                            layered(REPOSITORY_PACKAGE))
                    .because("controllers must delegate to services, "
                            + "never call repositories directly");

    /**
     * Controllers may depend on services (mandatory) and the rest of
     * the layers, but never on infrastructure-only modules (none
     * exist in the Layered convention so this is a defensive rule).
     */
    @ArchTest
    private final ArchRule controllers_depend_only_on_allowed_layers =
            classes().that().resideInAPackage(layered(CONTROLLER_PACKAGE))
                    .should().onlyAccessClassesThat()
                    .resideInAnyPackage(
                            layered(CONTROLLER_PACKAGE),
                            layered(SERVICE_PACKAGE),
                            layered(REPOSITORY_PACKAGE),
                            layered(ENTITY_PACKAGE),
                            layered(DTO_PACKAGE),
                            layered(SHARED_PACKAGE),
                            "java..",
                            "jakarta..",
                            "org.springframework..",
                            "io.quarkus..",
                            "org.junit..",
                            "org.mockito..",
                            "org.assertj..")
                    .because("controllers are the driving adapter layer; "
                            + "their dependencies must be limited to "
                            + "the documented Layered packages");

    /**
     * Services are the orchestration layer: they may consume
     * repositories, entities and shared, but never reach into
     * controllers (which would invert the dependency direction).
     */
    @ArchTest
    private final ArchRule services_must_not_depend_on_controllers =
            noClasses().that().resideInAPackage(layered(SERVICE_PACKAGE))
                    .should().dependOnClassesThat().resideInAPackage(
                            layered(CONTROLLER_PACKAGE))
                    .because("services must not know about HTTP concerns");

    /**
     * Repositories can depend on entities and shared but never on
     * services or controllers — they live at the bottom of the
     * dependency graph.
     */
    @ArchTest
    private final ArchRule repositories_must_not_depend_on_services =
            noClasses().that().resideInAPackage(layered(REPOSITORY_PACKAGE))
                    .should().dependOnClassesThat().resideInAnyPackage(
                            layered(CONTROLLER_PACKAGE),
                            layered(SERVICE_PACKAGE))
                    .because("repositories are the lowest layer; "
                            + "they must not depend on anything above them");

    /**
     * Entities are POJOs / records. They must not import anything
     * from {@code controller}, {@code service}, {@code repository}
     * or {@code dto} packages — entities stay pure.
     */
    @ArchTest
    private final ArchRule entities_must_not_depend_on_any_other_layer =
            noClasses().that().resideInAPackage(layered(ENTITY_PACKAGE))
                    .should().dependOnClassesThat().resideInAnyPackage(
                            layered(CONTROLLER_PACKAGE),
                            layered(SERVICE_PACKAGE),
                            layered(REPOSITORY_PACKAGE),
                            layered(DTO_PACKAGE))
                    .because("entities are framework-agnostic data; "
                            + "they must remain free of business logic");

    /**
     * DTOs (data transfer objects used by the HTTP layer) must not
     * import entities — this prevents accidental leakage of
     * persistence concerns into the transport contract.
     */
    @ArchTest
    private final ArchRule dtos_must_not_depend_on_entities =
            noClasses().that().resideInAPackage(layered(DTO_PACKAGE))
                    .should().dependOnClassesThat().resideInAPackage(
                            layered(ENTITY_PACKAGE))
                    .because("DTOs are the transport contract; "
                            + "they must stay decoupled from persistence");

    // ------------------------------------------------------------------
    // Layered coding rules
    // ------------------------------------------------------------------

    /**
     * Public methods on services must not declare {@code throws
     * Exception} — this masks domain errors that should propagate as
     * typed domain exceptions instead.
     */
    @ArchTest
    private final ArchRule services_should_not_throw_generic_exception =
            methods().that().areDeclaredInClassesThat()
                    .resideInAPackage(layered(SERVICE_PACKAGE))
                    .and().arePublic()
                    .should().notDeclareThrowableOfType(Exception.class)
                    .because("services must propagate typed domain "
                            + "exceptions, not generic java.lang.Exception");

    /**
     * Services must not expose public mutable fields. The rule keeps
     * services immutable and pushes any state into explicit methods
     * (or into the constructor for collaborators).
     *
     * <p>Framework-specific injection checks (e.g. forbidding
     * {@code @Autowired} fields in Spring Boot or {@code @Inject}
     * fields in Quarkus) are intentionally NOT hardcoded here so
     * this library stays framework-agnostic. Add them in your
     * subclass if needed.
     */
    @ArchTest
    private final ArchRule services_should_not_have_mutable_fields =
            noFields().that().areDeclaredInClassesThat()
                    .resideInAPackage(layered(SERVICE_PACKAGE))
                    .and().areNotStatic()
                    .should().notBeFinal()
                    .because("services should be immutable; use "
                            + "constructor injection for collaborators");

    /**
     * Inherit the standard ArchUnit coding rules so that common
     * pitfalls (print stack traces, use {@code System.out}, etc.) are
     * also flagged at build time.
     */
    @ArchTest
    private final ArchRule no_class_should_throw_generic_exceptions =
            GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;

    /**
     * Convenience hook for subclasses: called once at class-load
     * time with the imported {@link JavaClasses}. Override to add
     * extra assertions that aren't expressible as {@link ArchTest}
     * rules.
     *
     * @param importedClasses the classes ArchUnit scanned.
     */
    protected void additionalChecks(final JavaClasses importedClasses) {
        // intentionally empty — subclasses may override
    }
}