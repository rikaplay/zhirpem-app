"use client";

import React from 'react';
import {
    ArrowLeft, User, AtSign, Shield, Eye, Image as ImageIcon,
    Users, CheckCircle2, Lock, Palette, Smartphone, Droplets,
    Gauge, Type, Rocket, Volume2, MessageSquare, BadgeCheck,
    Music, Bell, Vibrating, Trash2, Zap, HelpCircle, Info, RefreshCw, Share2
} from 'lucide-react';

export const SettingsScreen = ({ onClose, user, onLogout }: any) => {
  const Section = ({ title, children }: any) => (
    <div className="mb-6">
      <h2 className="text-primary font-black text-sm uppercase tracking-tight mb-3 ml-4">{title}</h2>
      <div className="bg-white dark:bg-zinc-900 rounded-[32px] overflow-hidden shadow-sm border border-zinc-500/5 divide-y divide-zinc-500/5">
        {children}
      </div>
    </div>
  );

  const Item = ({ icon: Icon, label, value, color, hasArrow = true, isSwitch = false, checked = false }: any) => (
    <div className="p-4 flex items-center justify-between hover:bg-zinc-50 dark:hover:bg-zinc-800/30 transition-colors cursor-pointer group">
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
        <span className="text-zinc-300 group-hover:text-zinc-500 group-hover:translate-x-0.5 transition-all">❯</span>
      )}
    </div>
  );

  return (
    <div className="fixed inset-0 z-[70] bg-background-light dark:bg-background-dark flex flex-col animate-in slide-in-from-bottom duration-500 pb-20 overflow-y-auto">
      <div className="p-6 flex items-center gap-6 sticky top-0 bg-background-light/90 dark:bg-background-dark/90 backdrop-blur-md z-10">
        <button onClick={onClose} className="p-1 hover:bg-zinc-100 dark:hover:bg-zinc-800 rounded-full"><ArrowLeft size={24} /></button>
        <h1 className="text-2xl font-black tracking-tight">Настройки</h1>
      </div>

      <div className="px-4">
        <Section title="Аккаунт">
            <Item icon={User} label="Имя" value={user.name} color="bg-blue-500" />
            <Item icon={AtSign} label="Юзернейм" value={`@${user.id}`} color="bg-cyan-500" />
            <Item icon={Shield} label="Безопасность" value="Код восстановления доступа" color="bg-zinc-400" />
        </Section>

        <Section title="Конфиденциальность">
            <Item icon={Eye} label="Последняя активность" value="Все" color="bg-green-500" />
            <Item icon={User} label="Фото профиля" value="Все" color="bg-blue-600" />
            <Item icon={Users} label="Скрыть подписки" color="bg-indigo-500" isSwitch />
            <Item icon={CheckCircle2} label="Отчеты о прочтении" color="bg-purple-600" isSwitch checked />
            <Item icon={Lock} label="Защита приложения" color="bg-black" isSwitch />
        </Section>

        <Section title="Внешний вид">
            <Item icon={Palette} label="Тема оформления" value="Светлая" color="bg-purple-500" />
            <Item icon={Smartphone} label="Иконка приложения" color="bg-green-600" />
            <Item icon={Droplets} label="Своя палитра" color="bg-orange-500" isSwitch />
            <Item icon={Gauge} label="Низкая производительность" color="bg-blue-700" isSwitch />
            <div className="p-5">
                <p className="font-bold text-[15px] mb-2 flex justify-between">Размер шрифта <span>150%</span></p>
                <div className="w-full h-1.5 bg-primary/20 rounded-full relative">
                    <div className="absolute top-0 bottom-0 left-0 w-[80%] bg-primary rounded-full" />
                    <div className="absolute top-1/2 left-[80%] -translate-y-1/2 w-4 h-4 bg-white border-2 border-primary rounded-full shadow-md" />
                </div>
            </div>
        </Section>

        <Section title="Запуск">
            <Item icon={Rocket} label="Splash Screen" color="bg-pink-500" isSwitch checked />
            <Item icon={Volume2} label="Звук запуска" color="bg-yellow-500" isSwitch checked />
        </Section>

        <Section title="Чаты и звонки">
            <Item icon={MessageSquare} label="Оформление чатов" color="bg-indigo-600" />
            <Item icon={BadgeCheck} label="Только проверенные" color="bg-green-500" isSwitch />
            <Item icon={Music} label="Мелодия вызова" value="Tune 2" color="bg-orange-500" />
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
