import type { MateCategory } from '../types/mate';
import './CategoryGrid.css';

interface Props {
  categories: MateCategory[];
  onSelect: (code: string) => void;
}

export default function CategoryGrid({ categories, onSelect }: Props) {
  return (
    <div className="category-grid">
      {categories.map((cat) => (
        <button key={cat.code} className="category-item" onClick={() => onSelect(cat.code)}>
          <span className="category-icon">{cat.icon}</span>
          <span className="category-name">{cat.name}</span>
        </button>
      ))}
    </div>
  );
}
