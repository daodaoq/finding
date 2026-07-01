import './FloatingActionButton.css';

interface Props {
  onClick: () => void;
}

export default function FloatingActionButton({ onClick }: Props) {
  return (
    <button className="fab" onClick={onClick}>
      ＋
    </button>
  );
}
