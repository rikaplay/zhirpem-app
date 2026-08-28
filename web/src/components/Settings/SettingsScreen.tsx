"use client";

import React, { useState, useEffect } from 'react';
import {
    ArrowLeft, User, AtSign, Eye,
    Users, CheckCircle2, Lock, Palette, Droplets,
    Rocket, Volume2, Bell, Trash2, Zap, HelpCircle, Info, RefreshCw, Share2, Smartphone
} from 'lucide-react';
import { db } from '@/lib/firebase';
import { doc, updateDoc } from 'firebase/firestore';

import { setCookie, getCookie, hapticFeedback } from '@/lib/utils';

export const SettingsScreen = ({ onClose, user, onLogout }: any) => {
  const [theme, setTheme] = useState('Светлая');
  const [isGlassEnabled, setIsGlassEnabled] = useState(true);
  const [isReadReceipts, setIsReadReceipts] = useState(true);
  const [isHideFollows, setIsHideFollows] = useState(false);

  // Editing states
  const [isEditingName, setIsEditingName] = useState(false);
  const [isEditingUsername, setIsEditingUsername] = useState(false);
  const [newName, setNewName] = useState(user.name);
  const [newUsername, setNewUsername] = useState(user.id);

  useEffect(() => {
    // Load from cookies
    const savedGlass = getCookie('glass_enabled') !== 'false';
    setIsGlassEnabled(savedGlass);

    const savedTheme = getCookie('app_theme');
    if (savedTheme === 'dark' || document.documentElement.classList.contains('dark')) {
        setTheme('Темная');
        document.documentElement.classList.add('dark');
    }

    setIsReadReceipts(user.readReceipts !== false);
    setIsHideFollows(user.hideFollows || false);
  }, [user]);

  const toggleTheme = () => {
    const isDark = document.documentElement.classList.toggle('dark');
    const newTheme = isDark ? 'Темная' : 'Светлая';
    setTheme(newTheme);
    setCookie('app_theme', isDark ? 'dark' : 'light');
    hapticFeedback(15);
  };

  const toggleGlass = () => {
    const newVal = !isGlassEnabled;
    setIsGlassEnabled(newVal);
    setCookie('glass_enabled', String(newVal));
    document.documentElement.classList.toggle('no-glass', !newVal);
    hapticFeedback(15);
  };

  const updateFirebaseSetting = async (field: string, value: any, setter?: Function) => {
    if (setter) setter(value);
    try {
      await updateDoc(doc(db, "users", user.id), { [field]: value });
    } catch (e) { console.error(e); }
  };

  const handleSaveName = async () => {
    await updateFirebaseSetting('name', newName);
    setIsEditingName(false);
  };

  const handleSaveUsername = async () => {
    await updateFirebaseSetting('username', newUsername);
    setIsEditingUsername(false);
  };

  const Section = ({ title, children }: any) => (
    <div className="mb-6">
      <h2 className="text-primary font-black text-sm uppercase tracking-tight mb-3 ml-4">{title}</h2>
      <div className="bg-white dark:bg-zinc-900 rounded-[32px] overflow-hidden shadow-sm border border-zinc-500/5 divide-y divide-zinc-500/5">
        {children}
      </div>
    </div>
  );

  const Item = ({ icon: Icon, label, value, color, hasArrow = true, isSwitch = false, checked = false, onClick }: any) => (
    <div
        className="p-4 flex items-center justify-between hover:bg-zinc-50 dark:hover:bg-zinc-800/30 transition-colors cursor-pointer group"
        onClick={onClick}
    >
      <div className="flex items-center gap-4">
        <div className={`w-9 h-9 rounded-[14px] ${color} flex items-center justify-center text-white`}>
            <Icon size={20} strokeWidth={2.5} />
        </div>
        <div className="flex flex-col">
            <span className="font-bold text-[15px] text-zinc-800 dark:text-zinc-200">{label}</span>
            {value && <span className="text-[12px] font-bold text-zinc-400 uppercase tracking-tighter">{value}</span>}
        </div>
      </div>
      {isSwitch ? (
        <div className={`w-12 h-6 rounded-full transition-colors relative ${checked ? 'bg-primary' : 'bg-zinc-300'}`}>
            <div className={`absolute top-1 w-4 h-4 bg-white rounded-full transition-all ${checked ? 'left-7' : 'left-1'}`} />
        </div>
      ) : hasArrow && (
        <span className="text-zinc-300 group-hover:text-zinc-500 group-hover:translate-x-0.5 transition-all font-bold">❯</span>
      )}
    </div>
  );

  return (
    <div className="fixed inset-0 z-[70] bg-background-light dark:bg-background-dark flex flex-col animate-in slide-in-from-bottom duration-500 pb-20 overflow-y-auto">
      <div className="p-6 flex items-center gap-6 sticky top-0 bg-background-light/90 dark:bg-background-dark/90 backdrop-blur-md z-10">
        <button onClick={onClose} className="p-1 hover:bg-zinc-100 dark:hover:bg-zinc-800 rounded-full transition-transform active:scale-90"><ArrowLeft size={24} /></button>
        <h1 className="text-2xl font-black tracking-tight uppercase tracking-tighter">Настройки</h1>
      </div>

      <div className="px-4">
        <Section title="Аккаунт">
            <Item icon={User} label="Имя" value={newName} color="bg-blue-500" onClick={() => setIsEditingName(true)} />
            <Item icon={AtSign} label="Юзернейм" value={`@${newUsername}`} color="bg-cyan-500" onClick={() => setIsEditingUsername(true)} />
        </Section>

        {isEditingName && (
            <div className="fixed inset-0 z-[80] bg-black/60 backdrop-blur-sm flex items-center justify-center p-6">
                <div className="bg-white dark:bg-zinc-900 rounded-[32px] p-8 w-full max-w-md shadow-2xl">
                    <h3 className="text-xl font-black mb-4 uppercase tracking-tighter">Изменить имя</h3>
                    <input
                        className="w-full bg-zinc-100 dark:bg-zinc-800 rounded-2xl p-4 outline-none font-bold mb-6"
                        value={newName} onChange={e => setNewName(e.target.value)}
                    />
                    <div className="flex gap-3">
                        <button onClick={() => setIsEditingName(false)} className="flex-1 py-4 font-bold text-zinc-500 uppercase">Отмена</button>
                        <button onClick={handleSaveName} className="flex-1 py-4 bg-primary text-white dark:text-zinc-900 rounded-2xl font-black uppercase">Сохранить</button>
                    </div>
                </div>
            </div>
        )}

        {isEditingUsername && (
            <div className="fixed inset-0 z-[80] bg-black/60 backdrop-blur-sm flex items-center justify-center p-6">
                <div className="bg-white dark:bg-zinc-900 rounded-[32px] p-8 w-full max-w-md shadow-2xl">
                    <h3 className="text-xl font-black mb-4 uppercase tracking-tighter">Изменить юзернейм</h3>
                    <input
                        className="w-full bg-zinc-100 dark:bg-zinc-800 rounded-2xl p-4 outline-none font-bold mb-6"
                        value={newUsername} onChange={e => setNewUsername(e.target.value.replace(/\s/g, '').toLowerCase())}
                    />
                    <div className="flex gap-3">
                        <button onClick={() => setIsEditingUsername(false)} className="flex-1 py-4 font-bold text-zinc-500 uppercase">Отмена</button>
                        <button onClick={handleSaveUsername} className="flex-1 py-4 bg-primary text-white dark:text-zinc-900 rounded-2xl font-black uppercase">Сохранить</button>
                    </div>
                </div>
            </div>
        )}

        <Section title="Конфиденциальность">
            <Item icon={Eye} label="Последняя активность" value="Все" color="bg-green-500" />
            <Item icon={User} label="Фото профиля" value="Все" color="bg-blue-600" />
            <Item icon={Users} label="Скрыть подписки" color="bg-indigo-500" isSwitch checked={isHideFollows} onClick={() => updateFirebaseSetting('hideFollows', !isHideFollows, setIsHideFollows)} />
            <Item icon={CheckCircle2} label="Отчеты о прочтении" color="bg-purple-600" isSwitch checked={isReadReceipts} onClick={() => updateFirebaseSetting('readReceipts', !isReadReceipts, setIsReadReceipts)} />
        </Section>

        <Section title="Внешний вид">
            <Item icon={Palette} label="Тема оформления" value={theme} color="bg-purple-500" onClick={toggleTheme} />
            <Item icon={Droplets} label="Эффект стекла" color="bg-cyan-600" isSwitch checked={isGlassEnabled} onClick={toggleGlass} />
        </Section>

        <Section title="Запуск">
            <Item icon={Rocket} label="Splash Screen" color="bg-pink-500" isSwitch checked />
            <Item icon={Volume2} label="Звук запуска" color="bg-yellow-500" isSwitch checked />
        </Section>

        <Section title="Система">
            <Item icon={Bell} label="Уведомления" value="Все" color="bg-red-500" />
            <div className="px-12 py-2 space-y-4">
                <div className="flex items-center gap-3">
                    <div className="w-5 h-5 rounded-full border-2 border-primary flex items-center justify-center">
                        <div className="w-2.5 h-2.5 rounded-full bg-primary" />
                    </div>
                    <span className="font-bold text-sm">Все</span>
                </div>
                <div className="flex items-center gap-3 opacity-50">
                    <div className="w-5 h-5 rounded-full border-2 border-zinc-400" />
                    <span className="font-bold text-sm">Читаемые</span>
                </div>
                <div className="flex items-center gap-3 opacity-50">
                    <div className="w-5 h-5 rounded-full border-2 border-zinc-400" />
                    <span className="font-bold text-sm">Никто</span>
                </div>
            </div>
            <Item icon={Smartphone} label="Вибрация" color="bg-green-500" isSwitch checked />
        </Section>

        <Section title="Поддержка и О приложении">
            <Item icon={HelpCircle} label="Написать в поддержку" color="bg-blue-600" />
            <Item icon={Info} label="FAQ / База знаний" color="bg-purple-600" />
            <Item icon={RefreshCw} label="Обновления" color="bg-blue-500" />
            <Item icon={Share2} label="Пригласить друзей" color="bg-green-500" />
        </Section>

        <button
          onClick={onLogout}
          className="w-full mt-4 p-5 bg-red-500/10 text-red-500 font-black rounded-[28px] active:scale-95 transition-all mb-12 uppercase tracking-widest text-sm"
        >
          Выйти из аккаунта
        </button>

        <div className="text-center pb-20 opacity-20 font-bold uppercase text-[10px] tracking-[0.2em]">
            Версия: 1.6.0.1
        </div>
      </div>
    </div>
  );
};
