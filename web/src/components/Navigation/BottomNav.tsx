"use client";

import React, { useEffect, useState } from 'react';

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
    <div className="fixed bottom-6 left-1/2 -translate-x-1/2 w-[92%] max-w-[420px] h-[76px] z-50">
      {/*
        Жидкое стекло на iOS:
        1. backdrop-blur-3xl для сильного размытия.
        2. bg-white/40 для прозрачности как в iOS Control Center.
        3. Насыщенная обводка border-white/30.
      */}
      <div className="relative w-full h-full glass rounded-[38px] shadow-[0_20px_50px_rgba(0,0,0,0.2)] flex items-center justify-around px-4 border-white/30 dark:border-white/10">

        {/* Liquid Indicator (LENS) */}
        <div
          className="absolute h-[58px] w-[20%] bg-primary/20 backdrop-blur-3xl rounded-[30px] border border-primary/30 transition-all duration-500 ease-[cubic-bezier(0.34,1.56,0.64,1)] shadow-inner"
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
            className={`relative z-10 text-[26px] p-2 transition-all duration-300 active:scale-75 ${
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
