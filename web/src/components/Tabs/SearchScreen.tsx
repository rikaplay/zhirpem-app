"use client";

import React, { useState } from 'react';
import { db } from '@/lib/firebase';
import { collection, query, where, getDocs, limit } from 'firebase/firestore';
import { Search as SearchIcon, X, Clock } from 'lucide-react';

export const SearchScreen = () => {
  const [searchTerm, setSearchQuery] = useState('');
  const [activeTab, setActiveTab] = useState('Посты');
  const [results, setResults] = useState<any[]>([]);
  const [history, setHistory] = useState(['Duraff', '67', '6', 'газан', 'газае', '#обновления']);

  const tabs = ['Посты', 'Люди', 'Комментарии'];

  const handleSearch = async (val: string) => {
    setSearchQuery(val);
    // ... логика поиска ...
  };

  return (
    <div className="flex flex-col min-h-screen bg-background-light dark:bg-background-dark animate-in fade-in duration-500">
      {/* Search Input */}
      <div className="p-4 pb-2">
        <div className="relative">
            <div className="absolute left-4 top-1/2 -translate-y-1/2 text-zinc-400">
                <SearchIcon size={20} />
            </div>
            <input
                type="text"
                placeholder="Найти посты, людей..."
                className="w-full bg-[#E2DFE9]/40 dark:bg-zinc-800/60 rounded-full py-4 pl-12 pr-12 font-bold outline-none"
                value={searchTerm}
                onChange={(e) => handleSearch(e.target.value)}
            />
            {searchTerm && (
                <button onClick={() => setSearchQuery('')} className="absolute right-4 top-1/2 -translate-y-1/2"><X size={18} /></button>
            )}
        </div>
      </div>

      {/* Tabs */}
      <div className="flex border-b border-zinc-500/10 mb-4 px-2">
        {tabs.map(tab => (
            <button
                key={tab}
                onClick={() => setActiveTab(tab)}
                className={`flex-1 py-3 font-bold text-sm relative transition-all ${activeTab === tab ? 'text-primary' : 'text-zinc-500'}`}
            >
                {tab}
                {activeTab === tab && <div className="absolute bottom-0 left-0 right-0 h-1 bg-primary rounded-t-full mx-6" />}
            </button>
        ))}
      </div>

      <div className="px-4">
        {searchTerm.length < 1 && (
            <div>
                <div className="flex justify-between items-center mb-4">
                    <h3 className="font-bold text-zinc-800 dark:text-zinc-200">Недавние запросы</h3>
                    <button className="text-red-500 font-bold text-sm">Очистить</button>
                </div>
                <div className="space-y-4">
                    {history.map(item => (
                        <div key={item} className="flex items-center gap-4 text-zinc-500 font-medium cursor-pointer py-1">
                            <Clock size={18} className="text-zinc-400" />
                            <span>{item}</span>
                        </div>
                    ))}
                </div>
            </div>
        )}

        {searchTerm.length > 0 && results.length === 0 && (
            <div className="text-center py-40 font-bold text-zinc-400">Ничего не найдено</div>
        )}
      </div>
    </div>
  );
};
