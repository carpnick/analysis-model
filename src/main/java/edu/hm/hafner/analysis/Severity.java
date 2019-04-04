package edu.hm.hafner.analysis;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.factory.Sets;

import com.google.errorprone.annotations.Immutable;

import edu.hm.hafner.util.Ensure;
import edu.hm.hafner.util.StringContainsUtils;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Severity of an issue. The predefined set of severities consists of an error and 3 warnings with priorities high,
 * normal, or low. Additional severities can be created if this set of severities is not sufficient. Note that new
 * instances are not cached by this class so that there might exist several severity instances with the same name.
 *
 * @author Ullrich Hafner
 */
@Immutable
public class Severity implements Serializable {
    private static final long serialVersionUID = 8921726169259131484L;

    /** An error, e.g. a compile error. */
    public static final Severity ERROR = new Severity("ERROR");
    /** A warning with priority high. Mapping of warning priorities is determined by the corresponding tool. */
    public static final Severity WARNING_HIGH = new Severity("HIGH");
    /** A warning with priority normal. Mapping of warning priorities is determined by the corresponding tool. */
    public static final Severity WARNING_NORMAL = new Severity("NORMAL");
    /** A warning with priority low. Mapping of warning priorities is determined by the corresponding tool. */
    public static final Severity WARNING_LOW = new Severity("LOW");

    private static final ImmutableSet<Severity> ALL_SEVERITIES
            = Sets.immutable.of(ERROR, WARNING_HIGH, WARNING_NORMAL, WARNING_LOW);

    /**
     * Creates a new {@link Severity} with the specified name. If the name is the same as the name of one of the
     * predefined severities, then this existing severity is returned.
     *
     * @param name
     *         the name of the severity
     *
     * @return the severity
     */
    public static Severity valueOf(final String name) {
        if (ERROR.equalsIgnoreCase(name)) {
            return ERROR;
        }
        if (WARNING_HIGH.equalsIgnoreCase(name)) {
            return WARNING_HIGH;
        }
        if (WARNING_NORMAL.equalsIgnoreCase(name)) {
            return WARNING_NORMAL;
        }
        if (WARNING_LOW.equalsIgnoreCase(name)) {
            return WARNING_LOW;
        }
        return new Severity(name);
    }

    /**
     * Converts a String severity to one of the predefined severities. If the provided String does not match then the default
     * severity will be returned.
     *
     * @param severity
     *         priority as a String
     * @param defaultValue
     *         default severity, if the specified String is {@code null} or is not a valid {@link Severity} name
     *
     * @return enumeration value
     */
    public static Severity valueOf(@Nullable final String severity, final Severity defaultValue) {
        if (severity == null || ALL_SEVERITIES.stream()
                .map(Severity::getName)
                .noneMatch(name -> name.equals(severity))) {
            return defaultValue;
        }
        return valueOf(severity);
    }

    /**
     * Converts a String severity to one of the predefined severities. If the provided String does not match (even
     * partly) then the default severity will be returned.
     *
     * @param severity
     *         the severity string
     *
     * @return mapped level.
     */
    public static Severity guessFromString(@Nullable final String severity) {
        if (StringContainsUtils.containsAnyIgnoreCase(severity, "error", "severe", "critical")) {
            return Severity.ERROR;
        }
        if (StringContainsUtils.containsAnyIgnoreCase(severity, "info", "note")) {
            return Severity.WARNING_LOW;
        }
        if (StringUtils.containsIgnoreCase(severity, "warning")) {
            return Severity.WARNING_NORMAL;
        }
        return Severity.WARNING_LOW;
    }

    /**
     * Gets the severities starting from the specified severity to {@link Severity#ERROR}.
     *
     * @param minimumSeverity
     *         the minimum priority
     *
     * @return the priorities starting from the specified priority
     */
    public static Collection<Severity> collectSeveritiesFrom(final Severity minimumSeverity) {
        List<Severity> priorities = new ArrayList<>();
        priorities.add(Severity.ERROR);
        if (minimumSeverity.equals(WARNING_HIGH)) {
            priorities.add(WARNING_HIGH);
        }
        else if (minimumSeverity.equals(WARNING_NORMAL)) {
            priorities.add(WARNING_HIGH);
            priorities.add(WARNING_NORMAL);
        }
        else if (minimumSeverity.equals(WARNING_LOW)) {
            priorities.add(WARNING_HIGH);
            priorities.add(WARNING_NORMAL);
            priorities.add(WARNING_LOW);
        }
        return priorities;
    }

    /**
     * Returns the set of predefined {@link Severity} instances.
     *
     * @return all predefined severities
     */
    public static ImmutableSet<Severity> getPredefinedValues() {
        return ALL_SEVERITIES;
    }

    private final String name;

    /**
     * Creates a new {@link Severity} with the specified name.
     *
     * @param name
     *         the name of the severity
     */
    public Severity(final String name) {
        Ensure.that(name).isNotBlank();

        this.name = name;
    }

    /**
     * Returns the name of the severity.
     *
     * @return the name of the severity
     */
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Checks if this instance has a name that is equal to the specified name.
     *
     * @param severityName
     *         the name to check
     *
     * @return {@code true} if this instance has the same name, {@code false} otherwise
     */
    public boolean equalsIgnoreCase(final String severityName) {
        return getName().equalsIgnoreCase(severityName);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Severity severity = (Severity) o;

        return name.equals(severity.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
