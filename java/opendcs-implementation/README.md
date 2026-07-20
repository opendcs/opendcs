It is permissible for other implementations to depend on the opendcs-implementation project.
It *is not* permissiable for opendcs-implementation to depend on other implementation projects.

This is to prevent circular dependencies.

Additionally Classes in this project should be final if it is difficult to extend for additional behavior 
and implementations should implement the interface directly.

If it is reasonable to extend behavior a class does not have to be marked final and should account for changes in
behavior in some way.