export { default as ModeIcons } from "../assets/mode-icons.svg?react";

interface IconProps {
  name: string;
  className?: string;
  width?: React.CSSProperties["width"];
  height?: React.CSSProperties["height"];
}

export const ModeIcon: React.FC<IconProps> = ({
  name,
  className,
  width = "1em",
  height = "1em",
}) => {
  return (
    <svg className={className} width={width} height={height}>
      <use href={`#${name}`}></use>
    </svg>
  );
};

export default ModeIcon;
