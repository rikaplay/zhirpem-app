"use client";

import React, { useState, useEffect } from 'react';
import { db } from '@/lib/firebase';
import { collection, query, where, getDocs, limit, orderBy } from 'firebase/firestore';
import { Search as SearchIcon, X, Clock, User } from 'lucide-react';

interface SearchScreenProps {
  onUserClick: (uid: string) => void;
}

export const SearchScreen: React.FC<SearchScreenProps> = ({ onUserClick }) => {
  const [searchTerm, setSearchQuery] = useState('');
  const [activeTab, setActiveTab] = useState('Люди'); // По умолчанию Люди, так как логика проще
  const [results, setResults] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [history, setHistory] = useState<string[]>([]);

  useEffect(() => {
    const saved = localStorage.getItem('search_history');
    if (saved) setHistory(JSON.parse(saved));
  }, []);

  const performSearch = async (val: string) => {
    if (val.length < 2) { setResults([]); return; }
    setLoading(true);
    try {
      const cleanSearch = val.toLowerCase().trim().replace('@', '');
      let q;

      if (activeTab === 'Люди') {
        q = query(
          collection(db, "users"),
          where("username", ">=", cleanSearch),
          where("username", "<=", cleanSearch + '\uf8ff'),
          limit(20)
        );
      } else {
        q = query(
            collection(db, "zhirpem_posts"),
            where("text", ">=", val),
            where("text", "<=", val + '\uf8ff'),
            limit(20)
        );
      }

      const snapshot = await getDocs(q);
      setResults(snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() })));

      // Save to history
      const newHistory = [val, ...history.filter(h => h !== val)].slice(0, 5);
      setHistory(newHistory);
      localStorage.setItem('search_history', JSON.stringify(newHistory));

    } catch (err) { console.error(err); }
    finally { setLoading(false); }
  };

  return (
    <div className="flex flex-col min-h-screen bg-background-light dark:bg-background-dark">
      <div className="p-4 pb-2">
        <div className="relative">
            <div className="absolute left-4 top-1/2 -translate-y-1/2 text-zinc-400"><SearchIcon size={20} /></div>
            <input
                type="text"
                placeholder="Поиск..."
                className="w-full bg-[#E2DFE9]/40 dark:bg-zinc-800/60 rounded-full py-4 pl-12 pr-12 font-bold outline-none"
                value={searchTerm}
                onChange={(e) => setSearchQuery(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && performSearch(searchTerm)}
            />
            {searchTerm && <button onClick={() => setSearchQuery('')} className="absolute right-4 top-1/2 -translate-y-1/2"><X size={18} /></button>}
        </div>
      </div>

      <div className="flex border-b border-zinc-500/10 mb-4 px-2">
        {['Посты', 'Люди', 'Комментарии'].map(tab => (
            <button key={tab} onClick={() => setActiveTab(tab)} className={`flex-1 py-3 font-bold text-sm relative transition-all ${activeTab === tab ? 'text-primary' : 'text-zinc-500'}`}>
                {tab}{activeTab === tab && <div className="absolute bottom-0 left-0 right-0 h-1 bg-primary rounded-t-full mx-6" />}
            </button>
        ))}
      </div>

      <div className="px-4 space-y-3">
        {searchTerm.length < 1 && history.length > 0 && (
            <div>
                <div className="flex justify-between items-center mb-4">
                    <h3 className="font-bold text-zinc-800 dark:text-zinc-200 text-sm">Недавние запросы</h3>
                    <button onClick={() => {setHistory([]); localStorage.removeItem('search_history');}} className="text-red-500 font-bold text-xs uppercase">Очистить</button>
                </div>
                {history.map(item => (
                    <div key={item} onClick={() => {setSearchQuery(item); performSearch(item);}} className="flex items-center gap-4 text-zinc-500 font-bold cursor-pointer py-2 hover:bg-primary/5 rounded-xl px-2 transition-colors">
                        <Clock size={16} className="text-zinc-300" /><span>{item}</span>
                    </div>
                ))}
            </div>
        )}

        {loading && <div className="flex justify-center p-10"><div className="animate-spin rounded-full h-8 w-8 border-4 border-primary border-t-transparent" /></div>}

        {results.map((item) => (
          <div key={item.id} onClick={() => activeTab === 'Люди' && onUserClick(item.username || item.id)} className="flex items-center gap-4 p-4 glass rounded-[28px] cursor-pointer">
            <div className="w-12 h-12 rounded-full bg-primary/10 overflow-hidden flex-shrink-0">
                {item.avatarUrl ? <img src={item.avatarUrl} className="w-full h-full object-cover" /> : <div className="w-full h-full flex items-center justify-center text-primary font-bold">{item.name?.charAt(0)}</div>}
            </div>
            <div className="flex-1">
                <h3 className="font-black text-sm">{item.name || item.text?.substring(0, 20)}</h3>
                <p className="text-zinc-500 text-xs font-bold">@{item.username || item.handle || 'post'}</p>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
