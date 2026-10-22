# AGENTS.md

Notes below are the things that are *not* inferable from the build files or that are
expensive to discover by trial and error. Follow existing patterns elsewhere in the code;
see the last section for review policy.

## Layout and the one dependency rule

Java/Gradle multi-project — build with `./gradlew`. Not all subprojects live under `java/`:
`install/`, `docs/`, `schema/`, `integrationtesting/` (as `:testing`) and `javascript/` are
subprojects too (see `settings.gradle`).

The rule you can't infer: code depends on a subproject's `-api` project, never its
implementation project. (None of these are JPMS modules — no `module-info.java`.)

Exception, not a violation: `:opendcs` is the legacy monolith (old `decodes.*` plus much of
`org.opendcs`) that everything still builds on; depending on it is normal.

## Patterns: a migration is in progress

Prefer the newest pattern over whatever is nearest in the file you're editing.

- Current DAO/DAI work: interfaces under `java/opendcs-api/src/main/java/org/opendcs/**/api`,
  implementations in `java/opendcs-implementation/.../impl` or `java/cwms-implementation/...`.
  Jdbi3-based; services are wired with `@AutoService` / NetBeans `Lookup`.
- Read `docs/source/dev-docs.rst` before writing database or REST code. It specifies
  `DataTransaction` (commit-on-close, `wrapErrors`), mapping `SQLException`s to
  `OpenDcsDataRuntimeException`/`OpenDcsDataConstraintException` in `SqlExceptionHandler`
  rather than in DAO methods, and `AppExceptionMapper` for REST error handling.
  Do not copy the older hand-rolled DAO style in `:opendcs`.
- Flyway: never edit a released `V*.sql`, add a new one; number it off the highest version in
  that engine's `schema/` dir.

## Style

Allman braces (opening brace on its own line, closing brace aligned with the opening) and no
tabs, per `config/checkstyle.xml`:

```java
public class Foo
{
    public void bar()
    {
        if (condition)
        {
            doSomething();
        }
    }
}
```

Do NOT use K&R style (`void bar() {`).

`./gradlew :opendcs:checkstyleMain` if you want it verified.

## Tests

Root `./gradlew test` runs ordinary unit tests plus `:opendcs-web-ui-react:testStorybook`.

The integration suite (`:testing:opendcs-tests`) is wired special, in its `build.gradle`:

- Its plain `test` task excludes `**/*`. One task per engine does the work:
  `testOpenDCSXML`, `testOpenDCSPostgres`, `testOpenDCSOracle`, `testCWMSOracle` — hyphens are
  stripped from the engine name, so `-Popendcs.test.engine=OpenDCS-XML` yields `testOpenDCSXML`.
- Those tasks are `enabled` only when `:testing:opendcs-tests:test` (or the specific task) is
  named **fully qualified** on the command line; anything less silently runs nothing.
- Omitting `-Popendcs.test.engine` defaults to **all four** engines, which will fail without
  Oracle/CWMS databases. `OpenDCS-XML` is the one that needs no external database.
- These suites build the REST API and web UI wars and `:install:installDist` first, so they are
  slow; add `-Pno.docs=true` to skip doc tasks.

## Pull request standards

Keep branches focused on a single purpose and reasonably small. If you find unrelated issues
during work, propose them as separate tasks/PRs. Try for 10 files or less per PR, exceptions
are ok for very simple changes that touch more files. Always be on the lookout for
opportunities to DRY up code.

Minimize unnecessary changes to make reviews easy, but it is fine to suggest refactorings with
big DRY wins — ask first.

Go easy on comments. A good name for a method/field is worth more than a comment; a simple
one-line comment at class level is great, otherwise add comments only when necessary or
requested.
