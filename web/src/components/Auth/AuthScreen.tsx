"use client";

import React, { useState } from 'react';
import { db } from '@/lib/firebase';
import { doc, getDoc } from 'firebase/firestore';

interface AuthScreenProps {
  onSuccess: (userData: any) => void;
}

export const AuthScreen: React.FC<AuthScreenProps> = ({ onSuccess }) => {
  const [isLogin, setIsLogin] = useState(true);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleAuth = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    const cleanUsername = username.toLowerCase().trim().replace('@', '');

    try {
      const userDoc = await getDoc(doc(db, "users", cleanUsername));

      if (userDoc.exists()) {
        const data = userDoc.data();
        if (data.password === password) {
          // Сохраняем сессию в localStorage как в приложении
          const userData = {
            username: cleanUsername,
            name: data.name,
            blueBadge: data.blueBadge || false,
            avatarUrl: data.avatarUrl || null
          };
          localStorage.setItem('user_session', JSON.stringify(userData));
          onSuccess(userData);
        } else {
          setError('Неверный пароль');
        }
      } else {
        setError('Пользователь не найден');
      }
    } catch (err: any) {
      setError('Ошибка сети или ключей Firebase');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen flex-col items-center justify-center p-6 bg-background-light dark:bg-background-dark text-zinc-900 dark:text-white">
      <div className="w-full max-w-md glass p-8 rounded-[32px] shadow-2xl">
        <div className="flex flex-col items-center mb-8">
          <h1 className="text-3xl font-black text-primary mb-2">Жирпем</h1>
          <p className="text-zinc-500 dark:text-zinc-400">С возвращением!</p>
        </div>

        <form onSubmit={handleAuth} className="space-y-4">
          <div>
            <label className="block text-sm font-bold mb-2 ml-2">Юзернейм</label>
            <input
              type="text"
              placeholder="username"
              className="w-full bg-white dark:bg-zinc-800 border-none rounded-2xl p-4 outline-none focus:ring-2 focus:ring-primary transition-all"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
            />
          </div>

          <div>
            <label className="block text-sm font-bold mb-2 ml-2">Пароль</label>
            <input
              type="password"
              placeholder="••••••••"
              className="w-full bg-white dark:bg-zinc-800 border-none rounded-2xl p-4 outline-none focus:ring-2 focus:ring-primary transition-all"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>

          {error && <p className="text-red-500 text-sm font-bold text-center">{error}</p>}

          <button
            type="submit"
            disabled={loading}
            className="w-full bg-primary text-white dark:text-zinc-900 py-4 rounded-2xl font-black text-lg active:scale-95 transition-all shadow-lg disabled:opacity-50"
          >
            {loading ? 'Вход...' : 'Войти в Жирпем'}
          </button>
        </form>
      </div>
    </div>
  );
};
