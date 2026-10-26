.. _algorithm-updates:

Updating algorithm and computation definitions
==============================================

After installing updated OpenDCS algorithm classes, use ``algoupdate`` to
synchronize their declared inputs, outputs, and properties with the database.
The updater also repairs existing computations when an algorithm declares a
previous parameter or property name. It preserves existing time-series
assignments during unambiguous renames, comments, and configured property values.
It does not run computations or recalculate time-series data.

Run these commands from the installed OpenDCS ``bin`` directory, or add that
directory to your ``PATH``. Use a build containing the updater fixes; starting
an older installation's editor does not initialize the updater's separate
Oracle session.

Preview and apply
-----------------

The following examples use a local properties file and a password already set
in the ``DB_PASSWORD`` environment variable. Replace ``APP_USER`` with your
database account. Both ``-username`` and ``-password`` are currently required;
the command does not prompt for missing credentials.

Preview updates for all installed annotated algorithms:

.. code-block:: bash

   algoupdate -P "/path/to/user.properties" \
     -username APP_USER -password "$DB_PASSWORD"

Preview updates for one algorithm:

.. code-block:: bash

   algoupdate -P "/path/to/user.properties" \
     -username APP_USER -password "$DB_PASSWORD" \
     -algorithm "ResEvapAlgo"

Apply the reviewed updates for that algorithm:

.. code-block:: bash

   algoupdate -P "/path/to/user.properties" \
     -username APP_USER -password "$DB_PASSWORD" \
     -algorithm "ResEvapAlgo" -apply

``-algorithm`` is optional. Its value is the algorithm's name in the database,
not a computation name or a fully qualified Java class name. Omit it to process
all installed annotated algorithms. ``-apply`` is also optional: without it,
the command previews changes and rolls back the transaction. With it, the
command commits the changes if synchronization succeeds. A failure rolls back
the transaction.

The properties argument is a local file path; do not prefix it with
``username@hostname:``. The updated Unix launcher forwards ``-algorithm`` and
``-apply`` unchanged, so a ``--`` separator is no longer required for
``algoupdate``. An explicit leading ``--`` is still accepted for compatibility.

On Windows, use ``algoupdate.bat`` and the Windows environment-variable syntax:

.. code-block:: bat

   algoupdate.bat -P "C:\OpenDCS\user.properties" -username APP_USER -password "%DB_PASSWORD%" -algorithm "ResEvapAlgo" -apply

CWMS office and permissions
---------------------------

For CWMS, the properties file must select the correct database and office, for
example:

.. code-block:: properties

   editDatabaseType: CWMS
   editDatabaseLocation: jdbc:oracle:thin:@//db-host:1521/service-name
   CwmsOfficeId: NWO

Use your actual JDBC URL and authorized office. The updater initializes each
CWMS connection's office/security context using the account's assigned CCP
privileges. The account needs permission to read and modify the affected
algorithm and computation records for that office. Full DBA privileges are
not required, and the updater does not grant privileges.

If ``ORA-28113`` occurs on ``CP_ALGORITHM``, confirm that the installation
contains the CWMS session-initialization fix and that the profile selects the
intended office. Have the CWMS administrator inspect the Oracle policy trace
if it persists. A successful login in another application does not initialize
this command's connection.

Reviewing results
-----------------

Changes and conflicts are reported in the algorithm-update summaries; the
command uses ``algoupdate.log`` by default. A nonzero ``needsReview`` count
means the updater could not safely choose between existing assignments or
properties. It preserves conflicting entries rather than guessing. Review
those records before relying on the affected computation's outputs. Rerunning
the updater does not automatically resolve an ambiguity.

For ResEvap, the declared migrations include ``hourlyEvap`` to
``hourlyEvapRate`` and ``dailyEvap`` to ``dailyEvapDepth``. Newly added outputs
such as ``hourlyEvapDepth`` still need a time-series assignment if you want to
save them.

The 7.0 Swing editor has no **Update parameters** action, and the computation
parameter dialog does not allow renaming an existing role. Use the updater for
these migrations; changing an algorithm definition alone does not migrate the
existing computation assignments.

Running through the database manager
------------------------------------

On an existing database, ``manageDatabase`` runs algorithm synchronization
after applying schema migrations. It also runs synchronization when the schema
is already current. Unlike ``algoupdate``, this path applies changes; use
``algoupdate`` without ``-apply`` for a preview first.

For example, on Unix:

.. code-block:: bash

   manageDatabase -- -I CWMS-Oracle -P "/path/to/user.properties" \
     -username SCHEMA_OWNER -password "$SCHEMA_PASSWORD" \
     -appUsername APP_USER -appPassword "$APP_PASSWORD"

The ``--`` separator prevents the Unix launcher from interpreting the
``-appUsername`` and ``-appPassword`` arguments as its own ``-a`` option.
The current database-manager parser requires all four credential arguments,
even for an existing database. ``-appUsername`` and ``-appPassword`` are used
when creating the initial application account on a fresh installation.

Schema migrations use the ``-username`` account and require the corresponding
schema-migration permissions. After-update tasks also use that account, with
CWMS office/security context initialized from the profile and its assigned
privileges. The ``-appUsername`` account is not substituted for after-update
tasks. To update algorithms using an application account without running
schema migrations, use ``algoupdate`` instead.
