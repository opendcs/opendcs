The directories in here that aren't gradle sub projects haven't been converted yet.

Sub projects that are converted are flyway migration scripts under the `src/main/resources/db/<impl name>`.
at this time it is assumes java migrations would be the same package name with the `<impl name>` which may not be
not be a valid java package name. This will be addressed if it ever causes an issue.
