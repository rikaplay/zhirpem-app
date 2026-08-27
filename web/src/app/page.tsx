"use client";

import { useEffect, useState } from "react";
import { MainFeed } from "@/components/Feed/MainFeed";
import { AuthScreen } from "@/components/Auth/AuthScreen";
import { Sidebar } from "@/components/Navigation/Sidebar";
import { BottomNav } from "@/components/Navigation/BottomNav";
import { Plus, Bell, Menu } from "lucide-react";

export default function Home() {
  const [session, setSession] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const [activeBottomTab, setActiveBottomTab] = useState('home');

  useEffect(() => {
    const savedSession = localStorage.getItem('user_session');
    if (savedSession) {
      setSession(JSON.parse(savedSession));
    }
    setLoading(false);
  }, []);

  const handleLogout = () => {
    localStorage.removeItem('user_session');
    setSession(null);
    setIsSidebarOpen(false);
  };

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
    <main className="min-h-screen bg-background-light dark:bg-background-dark text-zinc-900 dark:text-zinc-100">

      {/* SIDEBAR (DRAWER) */}
      <Sidebar
        isOpen={isSidebarOpen}
        onClose={() => setIsSidebarOpen(false)}
        user={{
            id: session.username,
            username: session.username,
            name: session.name,
            avatarUrl: session.avatarUrl,
            uid: session.username,
            status: 'online',
            currentScreen: 'Main'
        }}
        onLogout={handleLogout}
      />

      {/* TOP BAR */}
      <header className="sticky top-0 z-30 bg-background-light/80 dark:bg-background-dark/80 backdrop-blur-xl px-4 py-4 flex items-center justify-between border-b border-zinc-500/5">
        <button
          onClick={() => setIsSidebarOpen(true)}
          className="w-11 h-11 glass rounded-2xl flex items-center justify-center active:scale-90 transition-transform"
        >
          {session.avatarUrl ? (
            <img src={session.avatarUrl} className="w-full h-full rounded-2xl object-cover" alt="Me" />
          ) : (
            <Menu size={22} className="text-primary" />
          )}
        </button>

        <div className="flex flex-col items-center">
            <span className="text-xl font-black tracking-tighter text-primary">ЖИРПЕМ</span>
            <div className="h-1 w-6 bg-primary/20 rounded-full" />
        </div>

        <button className="w-11 h-11 glass rounded-2xl flex items-center justify-center active:scale-90 transition-transform relative">
          <Bell size={22} className="text-zinc-400" />
          <span className="absolute top-2.5 right-2.5 w-2.5 h-2.5 bg-primary rounded-full border-2 border-white dark:border-zinc-900" />
        </button>
      </header>

      {/* MAIN CONTENT (FEED) */}
      <div className="max-w-[500px] mx-auto">
        {activeBottomTab === 'home' && <MainFeed myUsername={session.username} />}

        {activeBottomTab !== 'home' && (
          <div className="flex flex-col items-center justify-center py-40 opacity-30">
            <span className="text-6xl mb-4">🚧</span>
            <p className="font-bold text-xl uppercase tracking-widest">В разработке</p>
          </div>
        )}
      </div>

      {/* FAB (Floating Action Button) */}
      <button className="fixed right-6 bottom-28 w-16 h-16 bg-primary text-white dark:text-zinc-900 rounded-[24px] shadow-2xl flex items-center justify-center active:scale-90 transition-all hover:rotate-12 z-40 border-t border-white/20">
        <Plus size={32} strokeWidth={3} />
      </button>

      {/* BOTTOM NAVIGATION */}
      <BottomNav activeTab={activeBottomTab} onTabChange={setActiveBottomTab} />

    </main>
  );
}
