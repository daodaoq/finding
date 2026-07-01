import './SearchBar.css';

interface Props {
  placeholder?: string;
  onSearch?: (keyword: string) => void;
}

export default function SearchBar({ placeholder = '搜搭子', onSearch }: Props) {
  return (
    <div className="search-bar">
      <span className="search-loc">📍</span>
      <input
        className="search-input"
        type="text"
        placeholder={placeholder}
        onKeyDown={(e) => {
          if (e.key === 'Enter') onSearch?.(e.currentTarget.value);
        }}
      />
      <button className="search-btn" onClick={() => {}}>
        🔍
      </button>
    </div>
  );
}
