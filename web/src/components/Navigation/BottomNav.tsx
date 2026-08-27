"use client";

import React, { useState } from 'react';

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

  return (
    <div className="fixed bottom-6 left-1/2 -translate-x-1/2 w-[90%] max-w-[400px] h-[72px] z-50">
      <div className="relative w-full h-full glass rounded-full shadow-2xl flex items-center justify-around px-4 border-white/20">

        {/* Liquid Indicator (LENS) */}
        <div
          className="absolute h-[54px] w-[20%] bg-primary/20 backdrop-blur-3xl rounded-full border border-primary/30 transition-all duration-500 ease-[cubic-bezier(0.34,1.56,0.64,1)]"
          style={{
            left: `${items.findIndex(i => i.id === activeTab) * 25}%`,
            marginLeft: '2.5%'
          }}
        />

        {/* Icons */}
        {items.map((item) => (
          <button
            key={item.id}
            onClick={() => onTabChange(item.id)}
            className={`relative z-10 text-2xl p-2 transition-all duration-300 ${
              activeTab === item.id ? 'scale-125 grayscale-0' : 'grayscale opacity-40 hover:opacity-100 hover:grayscale-0'
            }`}
          >
            {item.icon}
          </button>
        ))}
      </div>
    </div>
  );
};
