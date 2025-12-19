import icons from '../assets/mode-icons.svg';

interface IconProps {
    name: string,
    className?: string
}

const ModeIcon: React.FC<IconProps> = ({name, className}) => {
    return (
        <svg className={className}>
            <use xlinkHref={`${icons}#${name}`} />
        </svg>
    );
};

export default ModeIcon;