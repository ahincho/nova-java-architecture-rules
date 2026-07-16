package pe.edu.nova.java.archunit;

import java.util.Locale;

/**
 * Enumerates the architectural styles that the Nova Platform
 * meta-framework can enforce via {@code nova-architecture-rules}.
 *
 * <p>Each style maps to a different package layout and a different set
 * of dependency rules. The framework's templates and archetypes ship
 * with one of these values embedded in the generated application, so
 * the matching {@code *ArchitectureTest} abstract class can be activated
 * with no further configuration.
 *
 * <p>Concrete mapping:
 * <ul>
 *   <li>{@link #LAYERED}: traditional N-tier
 *       ({@code controller..}, {@code service..}, {@code repository..},
 *       {@code entity..}, {@code dto..}).</li>
 *   <li>{@link #CLEAN}: Uncle Bob's Clean Architecture (4 concentric
 *       rings: domain, application, infrastructure, api).</li>
 *   <li>{@link #HEXAGONAL}: Alistair Cockburn's Ports &amp; Adapters
 *       ({@code domain..}, {@code application..}, {@code adapters..}).</li>
 * </ul>
 *
 * @see LayeredArchitectureTest
 */
public enum NovaArchitectureStyle {

    /** Traditional layered / N-tier architecture. */
    LAYERED,

    /** Uncle Bob's Clean Architecture (4 rings). */
    CLEAN,

    /** Alistair Cockburn's Ports &amp; Adapters / Hexagonal. */
    HEXAGONAL;

    /**
     * Resolves the lower-case identifier used in CLI flags and
     * Maven / Gradle properties (e.g. {@code -Pstyle=layered}).
     *
     * @return the canonical lower-case name of this style.
     */
    public String identifier() {
        return name().toLowerCase(Locale.ROOT);
    }
}