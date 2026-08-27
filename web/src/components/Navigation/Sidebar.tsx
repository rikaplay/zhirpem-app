"use client";

import React from 'react';
import { User } from '@/types/chat';
import {
  User as UserIcon,
  Settings,
  Bookmark,
  Users,
  BarChart3,
  LogOut,
  X
} from 'lucide-react';

interface SidebarProps {
  isOpen: boolean;
  onClose: () => void;
  user: User;
  onLogout: () => void;
  onSettingsOpen: () => void;
  onProfileOpen: (username: string) => void; // Добавили
}

export const Sidebar: React.FC<SidebarProps> = ({ isOpen, onClose, user, onLogout, onSettingsOpen, onProfileOpen }) => {
  if (!isOpen) return null;

  const menuItems = [
    { icon: <UserIcon size={22} />, label: "Мой Профиль", color: "text-blue-500", onClick: () => onProfileOpen(user.id) },
    { icon: <Settings size={22} />, label: "Настройки", color: "text-zinc-500", onClick: onSettingsOpen },
    { icon: <Bookmark size={22} />, label: "Закладки", color: "text-primary", onClick: () => {} },
    { icon: <Users size={22} />, label: "Сообщества", color: "text-orange-500", onClick: () => {} },
    { icon: <BarChart3 size={22} />, label: "Статистика", color: "text-purple-500", onClick: () => {} },
  ];

  return (
    <div className="fixed inset-0 z-50 overflow-hidden">
      <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" onClick={onClose} />
      <div className="absolute left-4 top-4 bottom-4 w-[300px] glass rounded-[40px] shadow-2xl flex flex-col p-6 animate-in slide-in-from-left duration-300">
        <button onClick={onClose} className="absolute right-6 top-6 p-2 hover:bg-white/10 rounded-full"><X size={24} /></button>

        <div className="mt-8 mb-8 flex flex-col items-start cursor-pointer group" onClick={() => onProfileOpen(user.id)}>
          <div className="w-16 h-16 rounded-[20px] bg-primary/20 overflow-hidden mb-4 border-2 border-primary/30">
            {user.avatarUrl ? <img src={user.avatarUrl} className="w-full h-full object-cover" /> : <div className="w-full h-full flex items-center justify-center text-primary font-black text-2xl">{user.name.charAt(0)}</div>}
          </div>
          <h2 className="text-2xl font-black text-zinc-900 dark:text-white leading-tight">{user.name}</h2>
          <p className="text-zinc-500 font-bold">@{user.id}</p>
        </div>

        <nav className="flex-1 space-y-2">
          {menuItems.map((item, idx) => (
            <div key={idx} onClick={() => { item.onClick(); onClose(); }} className="flex items-center gap-4 p-4 rounded-2xl hover:bg-zinc-100 dark:hover:bg-white/10 active:scale-95 transition-all cursor-pointer group">
              <span className={`${item.color}`}>{item.icon}</span>
              <span className="font-bold text-zinc-700 dark:text-zinc-200">{item.label}</span>
            </div>
          ))}
        </nav>

        <button onClick={onLogout} className="mt-auto flex items-center gap-4 p-4 rounded-2xl bg-red-500/10 text-red-500 font-bold active:scale-95 transition-all"><LogOut size={22} /><span>Выйти</span></button>
      </div>
    </div>
  );
};
