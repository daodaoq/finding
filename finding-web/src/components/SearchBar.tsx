import { useState } from 'react';
import AppIcon from './AppIcon';
import './SearchBar.css';

interface Props { placeholder?: string; onSearch?: (keyword: string) => void; }

export default function SearchBar({ placeholder = '搜索搭子', onSearch }: Props) {
  const [keyword, setKeyword] = useState('');
  const submit = () => onSearch?.(keyword.trim());
  return <div className="search-bar">
    <AppIcon name="location" className="search-loc" size={18} />
    <input className="search-input" type="search" value={keyword} placeholder={placeholder} onChange={(event) => setKeyword(event.target.value)} onKeyDown={(event) => event.key === 'Enter' && submit()} />
    <button className="search-btn" onClick={submit} aria-label="搜索"><AppIcon name="search" size={19} /></button>
  </div>;
}
