package org.opendcs.database.impl.opendcs.dao;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * Oracle before 23ai rejects a select without a from clause, so every
 * {@code merge ... using (select :a a, ...)} source needs the {@code <dual>} token
 * (defined as "from dual" on Oracle). The integration tests run against Oracle 23ai
 * images, which accept the from-less select, so they can't catch a missing token;
 * a missing one broke creating configs and sites on 19c (#2199).
 */
class MergeSourceDualTest
{
    private static final Pattern MERGE_SOURCE =
            Pattern.compile("using\\s*\\(\\s*select\\b(.*?)\\bon\\s*\\(", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern FROM = Pattern.compile("\\bfrom\\b", Pattern.CASE_INSENSITIVE);

    @Test
    void test_merge_sources_have_dual() throws IOException
    {
        final List<String> missing = new ArrayList<>();
        final List<Path> sources;
        try (var files = Files.walk(Path.of("src/main/java")))
        {
            sources = files.filter(f -> f.toString().endsWith(".java")).toList();
        }

        for (var file : sources)
        {
            final var src = Files.readString(file);
            final var matcher = MERGE_SOURCE.matcher(src);
            while (matcher.find())
            {
                final var select = matcher.group(1);
                if (!select.contains("<dual>") && !FROM.matcher(select).find())
                {
                    final var line = src.substring(0, matcher.start()).split("\n", -1).length;
                    missing.add(file.getFileName() + ":" + line);
                }
            }
        }

        assertTrue(missing.isEmpty(), "merge source select without <dual>: " + missing);
    }
}
