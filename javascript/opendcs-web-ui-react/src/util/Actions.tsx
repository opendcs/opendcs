
/**
 * Interface that provides for various actions. All actions are
 * optional indicating that that action is not supported and those controls should
 * be disabled or not rendered.
 *
 * @param T Is the primary type that will be interacted with. The whole object
 * @param V can be either the same as T, or another type such as for a "key"
 */
export interface Actions<T, V=T> {
    /**
     * Peform appropriate operation to "save" and item
     * @param item The item to save
     * @returns nothing
     */
    save?: (item: T) => void;
    /**
     * Perform an appropriate operation to remove a given item from it's source
     * @param item can be full item, or a key type
     * @returns nothing
     */
    remove?: (item: V) => void;
    /**
     * Add a new item to a given source
     * @returns nothing
     */
    add?: (item?: T) => void;
    /**
     * Inform whatever needs to here it that this item needs to be editted
     * @param item The item, or a key to the item.
     * @returns nothing
     */
    edit?: (item: V) => void;
}