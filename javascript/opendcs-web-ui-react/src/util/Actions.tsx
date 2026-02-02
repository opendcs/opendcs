export interface SaveAction<T> {
  /**
   * Peform appropriate operation to "save" and item
   * @param item The item to save
   * @returns nothing
   */
  save?: (item: T) => void;
}

export interface EditAction<V> {
  /**
   * Inform whatever needs to here it that this item needs to be editted
   * @param item The item, or a key to the item.
   * @returns nothing
   */
  edit?: (item: V) => void;
}

export interface RemoveAction<V> {
  /**
   * Perform an appropriate operation to remove a given item from it's source
   * @param item can be full item, or a key type
   * @returns nothing
   */
  remove?: (item: V) => void;
}

export interface AddAction<T> {
  /**
   * Add a new item to a given source
   * @returns nothing
   */
  add?: (item?: T) => void;
}

/**
 * Tell parent whatever operations is currently going on should be terminated.
 * Example: Deciding *NOT* to save a value.
 */
export interface CancelAction<T> {
  cancel?: (item: T) => void;
}

/**
 * Interface that provides for various actions. All actions are
 * optional indicating that that action is not supported and those controls should
 * be disabled or not rendered.
 *
 * @param T Is the primary type that will be interacted with. The whole object
 * @param V can be either the same as T, or another type such as for a "key"
 */
export type CollectionActions<T, V = T> = SaveAction<T> &
  EditAction<V> &
  RemoveAction<V> &
  AddAction<T>;

export type ItemActions<T> = SaveAction<T> & EditAction<T> & RemoveAction<T>;

export type UiState = "new" | "edit" | "show" | undefined;
