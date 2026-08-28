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
import { BookmarksScreen } from "@/components/Tabs/BookmarksScreen";
import { CommunitiesScreen } from "@/components/Tabs/CommunitiesScreen";
import { StatsScreen } from "@/components/Tabs/StatsScreen";
import { UpdatesScreen } from "@/components/Tabs/UpdatesScreen";
import { Plus, Bell, Menu, Sparkles } from "lucide-react";

export default function Home() {
  const [session, setSession] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const [isComposeOpen, setIsComposeOpen] = useState(false);
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);
  const [isUpdatesOpen, setIsUpdatesOpen] = useState(false);
  const [profileUsername, setProfileUsername] = useState<string | null>(null);
  const [activeChatId, setActiveChatId] = useState<string | null>(null);
  const [activeBottomTab, setActiveBottomTab] = useState('home');

  useEffect(() => {
    const savedSession = localStorage.getItem('user_session');
    if (savedSession) { setSession(JSON.parse(savedSession)); }

    // Apply saved theme/glass settings
    const glass = localStorage.getItem('glass_enabled') !== 'false';
    document.documentElement.classList.toggle('no-glass', !glass);

    setLoading(false);
  }, []);

  if (loading) return (
    <div className="flex h-screen w-full items-center justify-center bg-background-light dark:bg-background-dark">
        <div className="h-10 w-10 animate-spin rounded-full border-4 border-primary border-t-transparent"></div>
    </div>
  );

  if (!session) return <AuthScreen onSuccess={setSession} />;

  const userObj = { id: session.username, username: session.username, name: session.name, avatarUrl: session.avatarUrl };

  const handleTabChange = (tab: string) => {
    setActiveBottomTab(tab);
    setProfileUsername(null);
  };

  return (
    <main className="min-h-screen bg-background-light dark:bg-background-dark text-zinc-900 dark:text-zinc-100 pb-32 transition-colors duration-300">

      {/* SIDEBAR */}
      <Sidebar
        isOpen={isSidebarOpen}
        onClose={() => setIsSidebarOpen(false)}
        user={userObj as any}
        onLogout={() => {localStorage.removeItem('user_session'); setSession(null);}}
        onSettingsOpen={() => {setIsSettingsOpen(true); setIsSidebarOpen(false);}}
        onProfileOpen={(uid) => {setProfileUsername(uid); setActiveBottomTab('profile'); setIsSidebarOpen(false);}}
        onTabOpen={(tab) => {setActiveBottomTab(tab); setIsSidebarOpen(false);}}
      />

      {/* OVERLAYS */}
      {isComposeOpen && <ComposePost user={userObj} onClose={() => setIsComposeOpen(false)} onSuccess={() => {}} />}
      {isSettingsOpen && <SettingsScreen user={userObj} onClose={() => setIsSettingsOpen(false)} onLogout={() => {localStorage.removeItem('user_session'); setSession(null);}} />}
      {isUpdatesOpen && <UpdatesScreen onBack={() => setIsUpdatesOpen(false)} />}
      {activeChatId && <ChatDetailScreen chatId={activeChatId} myUsername={session.username} onBack={() => setActiveChatId(null)} />}

      {/* TOP BAR */}
      <header className="sticky top-0 z-30 bg-background-light/80 dark:bg-background-dark/80 backdrop-blur-xl px-4 py-4 flex items-center justify-between border-b border-zinc-500/5">
        <button
          onClick={() => setIsSidebarOpen(true)}
          className="w-11 h-11 glass rounded-2xl flex items-center justify-center overflow-hidden active:scale-90 transition-all border-white/20 shadow-sm"
        >
          {session.avatarUrl ? (
            <img src={session.avatarUrl} className="w-full h-full object-cover" alt="Profile" />
          ) : (
            <Menu size={22} className="text-primary" />
          )}
        </button>

        <div className="flex flex-col items-center">
            <span className="text-xl font-black tracking-tighter text-primary italic">ЖИРПЕМ</span>
            <div className="h-0.5 w-8 bg-primary/20 rounded-full" />
        </div>

        <div className="flex gap-2">
            <button
                onClick={() => setIsUpdatesOpen(true)}
                className="w-11 h-11 glass rounded-2xl flex items-center justify-center active:scale-90 transition-all relative border-white/20"
            >
                <Sparkles size={20} className="text-primary animate-pulse" />
            </button>
            <button
                onClick={() => setActiveBottomTab('notifications')}
                className="w-11 h-11 glass rounded-2xl flex items-center justify-center relative active:scale-90 transition-all border-white/20"
            >
                <Bell size={22} className={activeBottomTab === 'notifications' ? 'text-primary' : 'text-zinc-400'} />
                <span className="absolute top-2.5 right-2.5 w-2.5 h-2.5 bg-red-500 rounded-full border-2 border-white dark:border-zinc-900" />
            </button>
        </div>
      </header>

      {/* CONTENT AREA */}
      <div className="max-w-[500px] mx-auto">
        {activeBottomTab === 'home' && <MainFeed myUsername={session.username} myUser={userObj} onUserClick={setProfileUsername} />}
        {activeBottomTab === 'search' && <SearchScreen onUserClick={setProfileUsername} />}
        {activeBottomTab === 'notifications' && <NotificationsScreen myUsername={session.username} />}
        {activeBottomTab === 'messages' && <MessagesScreen myUsername={session.username} onChatClick={setActiveChatId} />}

        {/* Profile Views */}
        {(activeBottomTab === 'profile' || profileUsername) && (
            <UserProfileScreen
                username={profileUsername || session.username}
                myUser={userObj}
                onBack={() => {setProfileUsername(null); if(activeBottomTab === 'profile') setActiveBottomTab('home');}}
            />
        )}

        {activeBottomTab === 'bookmarks' && <BookmarksScreen myUser={userObj} onUserClick={setProfileUsername} />}
        {activeBottomTab === 'communities' && <CommunitiesScreen />}
        {activeBottomTab === 'stats' && <StatsScreen />}
      </div>

      {/* FAB */}
      {activeBottomTab === 'home' && !isSidebarOpen && (
        <button
          onClick={() => setIsComposeOpen(true)}
          className="fixed right-6 bottom-28 w-16 h-16 bg-primary text-white rounded-[24px] shadow-2xl flex items-center justify-center active:scale-90 transition-all hover:rotate-12 z-40 border border-white/20"
        >
          <Plus size={32} strokeWidth={3} />
        </button>
      )}

      {/* BOTTOM NAV */}
      {!isSidebarOpen && !activeChatId && !isSettingsOpen && !isUpdatesOpen && (
        <BottomNav activeTab={activeBottomTab} onTabChange={handleTabChange} />
      )}

    </main>
  );
}
