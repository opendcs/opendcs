export default function InputRadio({ text, group, formId }) {
  return (
    <div className="form-check">
      <input
        className="form-check-input"
        type="radio"
        name={group}
        id={formId}
        value={formId}
      />
      <label className="form-check-label" htmlFor={formId}>
        {text}
      </label>
    </div>
  );
}
