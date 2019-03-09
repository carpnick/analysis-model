package edu.hm.hafner.analysis.parser;

import java.util.Optional;
import java.util.regex.Matcher;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.analysis.Issue;
import edu.hm.hafner.analysis.IssueBuilder;
import edu.hm.hafner.analysis.Severity;
import edu.hm.hafner.analysis.RegexpLineParser;

/**
 * A parser for the MSBuild/PcLint compiler warnings.
 *
 * @author Ullrich Hafner
 */
public class MsBuildParser extends RegexpLineParser {
    private static final long serialVersionUID = -2141974437420906595L;

    private static final String MS_BUILD_WARNING_PATTERN
            = "(?:^(?:.*)Command line warning ([A-Za-z0-9]+):\\s*(.*)\\s*\\[(.*)\\])|"
            + ANT_TASK + "(?:(?:\\s*\\d+>)?(?:(?:(?:(.*)\\((\\d*)(?:,(\\d+))?.*\\)|.*LINK)\\s*:|"
            + "(.*):)\\s*([A-z-_]*\\s?(?:[Nn]ote|[Ii]nfo|[Ww]arning|(?:fatal\\s*)?[Ee]rror))\\s*:?\\s*([A-Za-z0-9\\-_]+)"
            + "\\s*:\\s(?:\\s*([A-Za-z0-9.]+)\\s*:)?\\s*(.*?)(?: \\[([^\\]]*)[/\\\\][^\\]\\\\]+\\])?"
            + "|(.*)\\s*:.*error\\s*(LNK[0-9]+):\\s*(.*)))$";

    /**
     * Creates a new instance of {@link MsBuildParser}.
     */
    public MsBuildParser() {
        super(MS_BUILD_WARNING_PATTERN);
    }

    @Override
    protected  String preProcessContent(String content){

        //Remove ANSI Color contents based on MSBuild parameter: /consoleloggerparameters:ForceConsoleColor
        String newcontent= content.replaceAll(".*\u001B\\[[;\\d]*[ -/]*[@-~]","");
        return newcontent;
    }


    @Override
    protected Optional<Issue> createIssue(final Matcher matcher, final IssueBuilder builder) {
        builder.setFileName(determineFileName(matcher));

        if (StringUtils.isNotBlank(matcher.group(2))) {
            return builder.setLineStart(0)
                    .setCategory(matcher.group(1))
                    .setMessage(matcher.group(2))
                    .setSeverity(Severity.WARNING_NORMAL)
                    .buildOptional();
        }
        if (StringUtils.isNotBlank(matcher.group(13))) {
            return builder.setLineStart(0)
                    .setCategory(matcher.group(14))
                    .setMessage(matcher.group(15))
                    .setSeverity(Severity.WARNING_HIGH)
                    .buildOptional();
        }
        if (StringUtils.isNotEmpty(matcher.group(10))) {
            return builder.setLineStart(matcher.group(5))
                    .setColumnStart(matcher.group(6))
                    .setCategory(matcher.group(9))
                    .setType(matcher.group(10))
                    .setMessage(matcher.group(11))
                    .setSeverity(determinePriority(matcher))
                    .buildOptional();
        }

        String category = matcher.group(9);
        if ("Expected".matches(category)) {
            return Optional.empty();
        }
        return builder.setLineStart(matcher.group(5))
                .setColumnStart(matcher.group(6))
                .setCategory(category)
                .setMessage(matcher.group(11))
                .setSeverity(determinePriority(matcher))
                .buildOptional();

    }

    /**
     * Determines the name of the file that is cause of the warning.
     *
     * @param matcher
     *         the matcher to get the matches from
     *
     * @return the name of the file with a warning
     */
    private String determineFileName(final Matcher matcher) {
        String fileName;
        if (StringUtils.isNotBlank(matcher.group(3))) {
            fileName = matcher.group(3);
        }
        else if (StringUtils.isNotBlank(matcher.group(7))) {
            fileName = matcher.group(7);
        }
        else if (StringUtils.isNotBlank(matcher.group(13))) {
            fileName = matcher.group(13);
        }
        else {
            fileName = matcher.group(4);
        }
        if (StringUtils.isBlank(fileName)) {
            fileName = StringUtils.substringBetween(matcher.group(11), "'");
        }
        if (StringUtils.isBlank(fileName)) {
            fileName = "unknown.file";
        }

        String projectDir = matcher.group(12);
        if (canResolveRelativeFileName(fileName, projectDir)) {
            fileName = FilenameUtils.concat(projectDir, fileName);
        }
        if ("MSBUILD".equals(fileName.trim())) {
            fileName = "-";
        }
        return fileName;
    }

    private boolean canResolveRelativeFileName(final String fileName, final String projectDir) {
        return StringUtils.isNotBlank(projectDir) && FilenameUtils.getPrefixLength(fileName) == 0
                && !"MSBUILD".equals(fileName.trim());
    }

    /**
     * Determines the priority of the warning.
     *
     * @param matcher
     *         the matcher to get the matches from
     *
     * @return the priority of the warning
     */
    private Severity determinePriority(final Matcher matcher) {
        if (isOfType(matcher, "note") || isOfType(matcher, "info")) {
            return Severity.WARNING_LOW;
        }
        if (isOfType(matcher, "warning")) {
            return Severity.WARNING_NORMAL;
        }
        return Severity.WARNING_HIGH;
    }

    /**
     * Returns whether the warning type is of the specified type.
     *
     * @param matcher
     *         the matcher
     * @param type
     *         the type to match with
     *
     * @return {@code true} if the warning type is of the specified type
     */
    private boolean isOfType(final Matcher matcher, final String type) {
        return StringUtils.containsIgnoreCase(matcher.group(8), type);
    }
}

