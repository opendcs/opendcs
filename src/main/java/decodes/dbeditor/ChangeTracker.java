package decodes.dbeditor;

/**
 * The edit screens that modify database object will implement this interface.
 */
public interface ChangeTracker
{
	/**
	 * @return true if changes have been made to this screen since the last
	 * time it was saved.
	 */
	public boolean hasChanged();

	/**
	 * Saves the changes back to the database & reset the hasChanged flag.
	 * @return true if save was successful, false if aborted.
	 */
	public boolean saveChanges();
}
