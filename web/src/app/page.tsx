"use client";

import { useEffect, useState } from "react";
import { MainFeed } from "@/components/Feed/MainFeed";
import { AuthScreen } from "@/components/Auth/AuthScreen";

export default function Home() {
  const [session, setSession] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Проверяем сохраненную сессию при загрузке
    const savedSession = localStorage.getItem('user_session');
    if (savedSession) {
      setSession(JSON.parse(savedSession));
    }
    setLoading(false);
  }, []);

  if (loading) {
    return (
      <div className="flex h-screen w-full items-center justify-center bg-background-light dark:bg-background-dark">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent"></div>
      </div>
    );
  }

  if (!session) {
    return <AuthScreen onSuccess={(data) => setSession(data)} />;
  }

  return (
    <main className="min-h-screen bg-background-light dark:bg-background-dark">
      <MainFeed myUsername={session.username} />

      {/* Bottom Nav */}
      <nav className="fixed bottom-6 left-1/2 -translate-x-1/2 w-[90%] max-w-[400px] glass h-16 rounded-full flex items-center justify-around px-6 z-50 shadow-xl border-white/20">
        <button className="text-2xl hover:scale-110 transition-transform">🏠</button>
        <button className="text-2xl grayscale hover:grayscale-0 transition-all opacity-50">🔍</button>
        <button className="text-2xl grayscale hover:grayscale-0 transition-all opacity-50">🔔</button>
        <button className="text-2xl grayscale hover:grayscale-0 transition-all opacity-50">✉️</button>
      </nav>
    </main>
  );
}
