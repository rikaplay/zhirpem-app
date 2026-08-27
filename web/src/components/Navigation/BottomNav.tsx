"use client";

import React from 'react';

interface BottomNavProps {
  activeTab: string;
  onTabChange: (tab: string) => void;
}

export const BottomNav: React.FC<BottomNavProps> = ({ activeTab, onTabChange }) => {
  const items = [
    { id: 'home', icon: '🏠', label: 'Главная' },
    { id: 'search', icon: '🔍', label: 'Поиск' },
    { id: 'notifications', icon: '🔔', label: 'Уведомления' },
    { id: 'messages', icon: '✉️', label: 'Сообщения' },
  ];

  // We need to determine if current activeTab (which might be profile) maps to one of these
  const getActiveIndex = () => {
    const idx = items.findIndex(i => i.id === activeTab);
    return idx === -1 ? 0 : idx;
  };

  const activeIndex = getActiveIndex();

  return (
    <div className="fixed bottom-6 left-1/2 -translate-x-1/2 w-[92%] max-w-[420px] h-[76px] z-50">
      <div className="relative w-full h-full glass rounded-[38px] shadow-[0_20px_50px_rgba(0,0,0,0.2)] flex items-center justify-between px-2 border-white/30 dark:border-white/10">

        {/* Liquid Indicator (LENS) - Perfectly centered in each 25% sector */}
        <div
          className="absolute h-[60px] w-[calc(25%-12px)] bg-primary/20 backdrop-blur-3xl rounded-[32px] border border-primary/30 transition-all duration-500 ease-[cubic-bezier(0.34,1.56,0.64,1)] shadow-inner"
          style={{
            left: `${activeIndex * 25}%`,
            transform: 'translateX(6px)'
          }}
        />

        {/* Icons */}
        {items.map((item) => (
          <button
            key={item.id}
            onClick={() => onTabChange(item.id)}
            className={`relative z-10 flex-1 h-full flex items-center justify-center text-[26px] transition-all duration-300 active:scale-75 ${
              activeTab === item.id ? 'scale-125 grayscale-0' : 'grayscale opacity-30 hover:opacity-100 hover:grayscale-0'
            }`}
          >
            {item.icon}
          </button>
        ))}
      </div>
    </div>
  );
};
