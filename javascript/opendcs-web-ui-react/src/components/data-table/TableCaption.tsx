import type { ReactNode } from "react";

/** A button rendered into a table's caption toolbar. */
export interface CaptionButton {
  /** Stable key / identifier, also emitted as `data-caption-action`. */
  key: string;
  /** Visible label. Omit for an icon-only button. */
  text?: string;
  /** Accessible label — always descriptive, even when `text` is generic. */
  ariaLabel: string;
  /** Bootstrap icon class, e.g. `"bi-plus-lg"`. */
  icon?: string;
  /** Bootstrap button variant (without the `btn-` prefix). Defaults to `"secondary"`. */
  variant?: string;
  onClick: () => void;
}

interface TableCaptionProps {
  /** Table title, centered in the caption row. */
  title?: ReactNode;
  /** Toolbar buttons, right-aligned next to the title. */
  buttons?: CaptionButton[];
}

/**
 * The table's title and its toolbar in a single row, rendered inside the
 * `<caption>` so "add" controls sit directly against the table they act on
 * rather than floating in a DataTables toolbar row above it.
 */
export function TableCaption({
  title,
  buttons = [],
}: Readonly<TableCaptionProps>): React.ReactElement {
  return (
    <caption className="dt-caption">
      <div className="dt-caption__bar">
        <span className="dt-caption__title">{title}</span>
        {buttons.length > 0 && (
          <span className="dt-caption__actions">
            {buttons.map((b) => (
              <button
                key={b.key}
                type="button"
                className={`btn btn-sm btn-${b.variant ?? "secondary"} dt-caption__button`}
                data-caption-action={b.key}
                aria-label={b.ariaLabel}
                onClick={b.onClick}
              >
                {b.icon && <i className={`bi ${b.icon}`} aria-hidden="true" />}
                {b.text && <span>{b.text}</span>}
              </button>
            ))}
          </span>
        )}
      </div>
    </caption>
  );
}

export default TableCaption;
