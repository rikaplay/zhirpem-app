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
}

export const Sidebar: React.FC<SidebarProps> = ({ isOpen, onClose, user, onLogout }) => {
  if (!isOpen) return null;

  const menuItems = [
    { icon: <UserIcon size={22} />, label: "Мой Профиль", color: "text-blue-500" },
    { icon: <Settings size={22} />, label: "Настройки", color: "text-zinc-500" },
    { icon: <Bookmark size={22} />, label: "Закладки", color: "text-primary" },
    { icon: <Users size={22} />, label: "Сообщества", color: "text-orange-500" },
    { icon: <BarChart3 size={22} />, label: "Статистика", color: "text-purple-500" },
  ];

  return (
    <div className="fixed inset-0 z-50 overflow-hidden">
      {/* Overlay */}
      <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" onClick={onClose} />

      {/* Drawer */}
      <div className="absolute left-4 top-4 bottom-4 w-[300px] glass rounded-[40px] shadow-2xl flex flex-col p-6 animate-in slide-in-from-left duration-300">
        <button onClick={onClose} className="absolute right-6 top-6 p-2 hover:bg-white/10 rounded-full">
            <X size={24} />
        </button>

        {/* Profile Header */}
        <div className="mt-8 mb-8 flex flex-col items-start cursor-pointer group">
          <div className="w-16 h-16 rounded-[20px] bg-primary/20 overflow-hidden mb-4 border-2 border-primary/30 group-active:scale-95 transition-transform">
            {user.avatarUrl ? (
              <img src={user.avatarUrl} alt={user.name} className="w-full h-full object-cover" />
            ) : (
              <div className="w-full h-full flex items-center justify-center text-primary font-black text-2xl">
                {user.name.charAt(0).toUpperCase()}
              </div>
            )}
          </div>
          <h2 className="text-2xl font-black text-zinc-900 dark:text-white leading-tight">{user.name}</h2>
          <p className="text-zinc-500 font-bold">@{user.id}</p>
        </div>

        <div className="h-[1px] bg-zinc-500/10 w-full mb-6" />

        {/* Menu Items */}
        <nav className="flex-1 space-y-2">
          {menuItems.map((item, idx) => (
            <div
              key={idx}
              className="flex items-center gap-4 p-4 rounded-2xl hover:bg-white/10 active:scale-95 transition-all cursor-pointer group"
            >
              <span className={`${item.color} group-hover:scale-110 transition-transform`}>{item.icon}</span>
              <span className="font-bold text-zinc-700 dark:text-zinc-200">{item.label}</span>
            </div>
          ))}
        </nav>

        {/* Logout */}
        <button
          onClick={onLogout}
          className="mt-auto flex items-center gap-4 p-4 rounded-2xl bg-red-500/10 text-red-500 font-bold active:scale-95 transition-all"
        >
          <LogOut size={22} />
          <span>Выйти из аккаунта</span>
        </button>
      </div>
    </div>
  );
};
