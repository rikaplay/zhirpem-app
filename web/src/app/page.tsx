"use client";

import { useEffect, useState } from "react";
import { MainFeed } from "@/components/Feed/MainFeed";
import { AuthScreen } from "@/components/Auth/AuthScreen";
import { Sidebar } from "@/components/Navigation/Sidebar";
import { BottomNav } from "@/components/Navigation/BottomNav";
import { ComposePost } from "@/components/Feed/ComposePost";
import { SettingsScreen } from "@/components/Settings/SettingsScreen";
import { SearchScreen } from "@/components/Tabs/SearchScreen";
import { NotificationsScreen } from "@/components/Tabs/NotificationsScreen";
import { MessagesScreen } from "@/components/Tabs/MessagesScreen";
import { UserProfileScreen } from "@/components/Profile/UserProfileScreen";
import { ChatDetailScreen } from "@/components/Tabs/ChatDetailScreen";
import { Plus, Bell, Menu } from "lucide-react";

export default function Home() {
  const [session, setSession] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const [isComposeOpen, setIsComposeOpen] = useState(false);
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);
  const [activeBottomTab, setActiveBottomTab] = useState('home');
  const [activeChatId, setActiveChatId] = useState<string | null>(null);

  useEffect(() => {
    const savedSession = localStorage.getItem('user_session');
    if (savedSession) { setSession(JSON.parse(savedSession)); }
    setLoading(false);
  }, []);

  if (loading) return <div className="flex h-screen w-full items-center justify-center"><div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent"></div></div>;
  if (!session) return <AuthScreen onSuccess={setSession} />;

  const userObj = { id: session.username, username: session.username, name: session.name, avatarUrl: session.avatarUrl };

  const handleTabChange = (tab: string) => {
    setActiveBottomTab(tab);
    // If we were on profile view or other sub-states, we reset them here if needed
    // But profile is now a tab 'profile'
  };

  return (
    <main className="min-h-screen bg-background-light dark:bg-background-dark text-zinc-900 dark:text-zinc-100 pb-32">
      <Sidebar
        isOpen={isSidebarOpen}
        onClose={() => setIsSidebarOpen(false)}
        user={userObj as any}
        onLogout={() => {localStorage.removeItem('user_session'); setSession(null);}}
        onSettingsOpen={() => {setIsSettingsOpen(true); setIsSidebarOpen(false);}}
        onProfileOpen={() => {setActiveBottomTab('profile'); setIsSidebarOpen(false);}}
      />

      {isComposeOpen && <ComposePost user={userObj} onClose={() => setIsComposeOpen(false)} onSuccess={() => {}} />}
      {isSettingsOpen && <SettingsScreen user={userObj} onClose={() => setIsSettingsOpen(false)} onLogout={() => {localStorage.removeItem('user_session'); setSession(null);}} />}
      {activeChatId && <ChatDetailScreen chatId={activeChatId} myUsername={session.username} onBack={() => setActiveChatId(null)} />}

      <header className="sticky top-0 z-30 bg-background-light/80 dark:bg-background-dark/80 backdrop-blur-xl px-4 py-4 flex items-center justify-between border-b border-zinc-500/5">
        <button onClick={() => setIsSidebarOpen(true)} className="w-11 h-11 glass rounded-2xl flex items-center justify-center overflow-hidden">
          {session.avatarUrl ? <img src={session.avatarUrl} className="w-full h-full object-cover" /> : <div className="text-primary font-black">@</div>}
        </button>
        <div className="flex flex-col items-center">
            <span className="text-xl font-black tracking-tighter text-primary">ЖИРПЕМ</span>
            <div className="h-1 w-6 bg-primary/20 rounded-full" />
        </div>
        <button onClick={() => setActiveBottomTab('notifications')} className="w-11 h-11 glass rounded-2xl flex items-center justify-center relative">
          <Bell size={22} className={activeBottomTab === 'notifications' ? 'text-primary' : 'text-zinc-400'} />
        </button>
      </header>

      <div className="max-w-[500px] mx-auto">
        {activeBottomTab === 'home' && <MainFeed myUsername={session.username} myUser={userObj} onUserClick={(uid) => setActiveBottomTab('profile_'+uid)} />}
        {activeBottomTab === 'search' && <SearchScreen onUserClick={(uid) => setActiveBottomTab('profile_'+uid)} />}
        {activeBottomTab === 'notifications' && <NotificationsScreen myUsername={session.username} />}
        {activeBottomTab === 'messages' && <MessagesScreen myUsername={session.username} onChatClick={setActiveChatId} />}
        {activeBottomTab === 'profile' && <UserProfileScreen username={session.username} myUser={userObj} onBack={() => setActiveBottomTab('home')} />}
        {activeBottomTab.startsWith('profile_') && (
            <UserProfileScreen
                username={activeBottomTab.split('_')[1]}
                myUser={userObj}
                onBack={() => setActiveBottomTab('home')}
            />
        )}
      </div>

      {activeBottomTab === 'home' && (
        <button onClick={() => setIsComposeOpen(true)} className="fixed right-6 bottom-28 w-16 h-16 bg-primary text-white rounded-[24px] shadow-2xl flex items-center justify-center active:scale-90 transition-all hover:rotate-12 z-40 border-t border-white/20"><Plus size={32} strokeWidth={3} /></button>
      )}

      {/* Hide BottomNav when Sidebar is open */}
      {!isSidebarOpen && <BottomNav activeTab={activeBottomTab} onTabChange={handleTabChange} />}
    </main>
  );
}
