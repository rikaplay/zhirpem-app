"use client";

import { useEffect, useState } from "react";
import { MainFeed } from "@/components/Feed/MainFeed";
import { auth } from "@/lib/firebase";
import { onAuthStateChanged } from "firebase/auth";

export default function Home() {
  const [user, setUser] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (user) => {
      setUser(user);
      setLoading(false);
    });
    return () => unsubscribe();
  }, []);

  if (loading) {
    return (
      <div className="flex h-screen w-full items-center justify-center bg-background-light dark:bg-background-dark">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent"></div>
      </div>
    );
  }

  if (!user) {
    return (
      <div className="flex h-screen w-full flex-col items-center justify-center p-4 bg-background-light dark:bg-background-dark">
        <h1 className="text-3xl font-bold mb-6">Жирпем Web</h1>
        <button
          onClick={() => {/* Redirect to auth or show login component */}}
          className="bg-primary text-white dark:text-zinc-900 px-8 py-3 rounded-2xl font-bold active:scale-95 transition-all"
        >
          Войти в аккаунт
        </button>
      </div>
    );
  }

  return (
    <main className="min-h-screen bg-background-light dark:bg-background-dark">
      <MainFeed myUsername={user.displayName || user.uid} />

      {/* Bottom Nav Mock */}
      <nav className="fixed bottom-4 left-1/2 -translate-x-1/2 w-[90%] max-w-[400px] glass h-16 rounded-full flex items-center justify-around px-6 z-50">
        <button className="text-2xl text-primary">🏠</button>
        <button className="text-2xl text-zinc-400">🔍</button>
        <button className="text-2xl text-zinc-400">🔔</button>
        <button className="text-2xl text-zinc-400">✉️</button>
      </nav>
    </main>
  );
}
