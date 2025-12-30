export { default as ModeIcons } from "../assets/mode-icons.svg?react";

interface IconProps {
  name: string;
  className?: string;
}

export const ModeIcon: React.FC<IconProps> = ({ name, className }) => {
  return (
    <svg className={className}>
      <use href={`#${name}`}></use>
    </svg>
  );
};

export default ModeIcon;
