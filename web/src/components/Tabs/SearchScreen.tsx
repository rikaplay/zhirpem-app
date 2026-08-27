"use client";

import React, { useState, useEffect } from 'react';
import { db } from '@/lib/firebase';
import { collection, query, where, getDocs, limit } from 'firebase/firestore';
import { Search as SearchIcon, X, User } from 'lucide-react';

export const SearchScreen = () => {
  const [searchTerm, setSearchQuery] = useState('');
  const [results, setResults] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);

  const handleSearch = async (val: string) => {
    setSearchQuery(val);
    if (val.length < 2) {
      setResults([]);
      return;
    }

    setLoading(true);
    try {
      const cleanSearch = val.toLowerCase().trim().replace('@', '');
      const q = query(
        collection(db, "users"),
        where("username", ">=", cleanSearch),
        where("username", "<=", cleanSearch + '\uf8ff'),
        limit(10)
      );

      const snapshot = await getDocs(q);
      setResults(snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() })));
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex flex-col min-h-screen px-4 pt-4 animate-in fade-in duration-500">
      <div className="relative mb-6">
        <div className="absolute left-4 top-1/2 -translate-y-1/2 text-zinc-400">
          <SearchIcon size={20} />
        </div>
        <input
          type="text"
          placeholder="Поиск людей или @username"
          className="w-full bg-white dark:bg-zinc-900 rounded-[24px] py-4 pl-12 pr-12 font-bold outline-none shadow-sm focus:ring-2 focus:ring-primary/30 transition-all border border-zinc-100 dark:border-zinc-800"
          value={searchTerm}
          onChange={(e) => handleSearch(e.target.value)}
        />
        {searchTerm && (
          <button
            onClick={() => handleSearch('')}
            className="absolute right-4 top-1/2 -translate-y-1/2 p-1 bg-zinc-200 dark:bg-zinc-800 rounded-full hover:scale-110 transition-transform"
          >
            <X size={16} />
          </button>
        )}
      </div>

      <div className="flex-1 space-y-3">
        {loading && (
          <div className="flex justify-center p-12">
            <div className="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin"></div>
          </div>
        )}

        {results.map((user) => (
          <div
            key={user.id}
            className="flex items-center gap-4 p-4 glass rounded-[28px] hover:scale-[1.02] active:scale-95 transition-all cursor-pointer shadow-sm border-white/10"
          >
            <div className="w-14 h-14 rounded-full bg-primary/10 overflow-hidden border-2 border-white dark:border-zinc-800 flex-shrink-0">
              {user.avatarUrl ? (
                <img src={user.avatarUrl} className="w-full h-full object-cover" alt="" />
              ) : (
                <div className="w-full h-full flex items-center justify-center text-primary font-black text-xl">
                  {user.name?.charAt(0).toUpperCase() || '?'}
                </div>
              )}
            </div>
            <div className="flex-1">
              <h3 className="font-black text-[17px] text-zinc-900 dark:text-zinc-100">{user.name}</h3>
              <p className="text-zinc-500 font-bold text-sm">@{user.username}</p>
            </div>
            <button className="bg-primary text-white dark:text-zinc-900 px-5 py-2 rounded-full font-black text-xs uppercase tracking-wider shadow-md shadow-primary/20">
              Профиль
            </button>
          </div>
        ))}

        {!loading && searchTerm.length >= 2 && results.length === 0 && (
          <div className="text-center py-20 opacity-30 italic font-bold uppercase tracking-widest">
            Ничего не найдено
          </div>
        )}

        {searchTerm.length < 2 && (
          <div className="text-center py-24">
            <div className="text-6xl mb-6 grayscale opacity-20">🔍</div>
            <p className="text-zinc-400 font-black text-lg uppercase tracking-tighter italic">Найди кого-нибудь классного</p>
          </div>
        )}
      </div>
    </div>
  );
};
