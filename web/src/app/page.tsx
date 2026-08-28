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
import { Plus, Sparkles, Menu } from "lucide-react";
import { db } from "@/lib/firebase";
import { doc, onSnapshot } from "firebase/firestore";
import { getCookie } from "@/lib/utils";

export default function Home() {
  const [session, setSession] = useState<any>(null);
  const [userData, setUserData] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const [isComposeOpen, setIsComposeOpen] = useState(false);
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);
  const [isUpdatesOpen, setIsUpdatesOpen] = useState(false);
  const [profileUsername, setProfileUsername] = useState<string | null>(null);
  const [activeChatId, setActiveChatId] = useState<string | null>(null);
  const [activeBottomTab, setActiveBottomTab] = useState('home');

  useEffect(() => {
    const saved = localStorage.getItem('user_session');
    if (saved) {
        const s = JSON.parse(saved);
        setSession(s);
        // Live update user data
        onSnapshot(doc(db, "users", s.username), (snap) => {
            if (snap.exists()) setUserData({ id: snap.id, ...snap.data() });
        });
    }

    // Apply saved theme/glass settings from cookies
    const glass = getCookie('glass_enabled') !== 'false';
    document.documentElement.classList.toggle('no-glass', !glass);

    const theme = getCookie('app_theme');
    if (theme === 'dark') document.documentElement.classList.add('dark');

    setLoading(false);
  }, []);

  // Swipe Gesture Listener
  useEffect(() => {
    let touchStartX = 0;
    const handleTouchStart = (e: TouchEvent) => { touchStartX = e.touches[0].clientX; };
    const handleTouchEnd = (e: TouchEvent) => {
      const touchEndX = e.changedTouches[0].clientX;
      if (touchEndX - touchStartX > 100 && touchStartX < 50) { // Swipe from left edge
        setIsSidebarOpen(true);
      }
    };
    window.addEventListener('touchstart', handleTouchStart);
    window.addEventListener('touchend', handleTouchEnd);
    return () => {
      window.removeEventListener('touchstart', handleTouchStart);
      window.removeEventListener('touchend', handleTouchEnd);
    };
  }, []);

  // Poll for new events every 5s
  useEffect(() => {
    if (!session) return;
    const interval = setInterval(() => {
        // Logic to check for new notifications or posts could go here
    }, 5000);
    return () => clearInterval(interval);
  }, [session]);

  if (loading) return <div className="flex h-screen w-full items-center justify-center"><div className="h-10 w-10 animate-spin border-4 border-primary border-t-transparent rounded-full" /></div>;
  if (!session) return <AuthScreen onSuccess={setSession} />;

  return (
    <main className="min-h-screen bg-background-light dark:bg-background-dark text-zinc-900 dark:text-zinc-100 pb-32 overflow-hidden">

      {/* Sidebar with Gesture Placeholder */}
      <div
        className="fixed inset-y-0 left-0 w-4 z-50"
        onMouseEnter={() => setIsSidebarOpen(true)} // Simple gesture placeholder
      />

      <Sidebar
        isOpen={isSidebarOpen}
        onClose={() => setIsSidebarOpen(false)}
        user={userData || session}
        onLogout={() => {localStorage.removeItem('user_session'); window.location.reload();}}
        onSettingsOpen={() => setIsSettingsOpen(true)}
        onProfileOpen={(uid) => {setProfileUsername(uid); setActiveBottomTab('profile');}}
        onTabOpen={setActiveBottomTab}
      />

      {isComposeOpen && <ComposePost user={userData || session} onClose={() => setIsComposeOpen(false)} onSuccess={() => { if(window.navigator.vibrate) window.navigator.vibrate(50); }} />}
      {isSettingsOpen && <SettingsScreen user={userData || session} onClose={() => setIsSettingsOpen(false)} onLogout={() => {localStorage.removeItem('user_session'); window.location.reload();}} />}
      {isUpdatesOpen && <UpdatesScreen onBack={() => setIsUpdatesOpen(false)} myUser={userData || session} />}
      {activeChatId && <ChatDetailScreen chatId={activeChatId} myUsername={session.username} onBack={() => setActiveChatId(null)} />}

      <header className="sticky top-0 z-30 bg-background-light/80 dark:bg-background-dark/80 backdrop-blur-xl px-4 py-4 flex items-center justify-between border-b border-zinc-500/5">
        <button onClick={() => setIsSidebarOpen(true)} className="w-11 h-11 glass rounded-2xl flex items-center justify-center overflow-hidden">
          {(userData?.avatarUrl || session.avatarUrl) ? <img src={userData?.avatarUrl || session.avatarUrl} className="w-full h-full object-cover" /> : <Menu size={22} className="text-primary" />}
        </button>

        <div className="flex flex-col items-center">
            <span className="text-xl font-black tracking-tighter text-primary italic">ЖИРПЕМ</span>
            <div className="h-0.5 w-8 bg-primary/20 rounded-full" />
        </div>

        <button onClick={() => setIsUpdatesOpen(true)} className="w-11 h-11 glass rounded-2xl flex items-center justify-center relative active:scale-90 transition-all border-white/20">
          <Sparkles size={22} className="text-primary" />
        </button>
      </header>

      <div className="max-w-[500px] mx-auto min-h-screen">
        {activeBottomTab === 'home' && <MainFeed myUsername={session.username} myUser={userData || session} onUserClick={setProfileUsername} />}
        {activeBottomTab === 'search' && <SearchScreen onUserClick={setProfileUsername} />}
        {activeBottomTab === 'notifications' && <NotificationsScreen myUsername={session.username} />}
        {activeBottomTab === 'messages' && <MessagesScreen myUsername={session.username} onChatClick={setActiveChatId} />}
        {(activeBottomTab === 'profile' || profileUsername) && <UserProfileScreen username={profileUsername || session.username} myUser={userData || session} onBack={() => {setProfileUsername(null); setActiveBottomTab('home');}} />}
        {activeBottomTab === 'bookmarks' && <BookmarksScreen myUser={userData || session} onUserClick={setProfileUsername} />}
        {activeBottomTab === 'communities' && <CommunitiesScreen />}
        {activeBottomTab === 'stats' && <StatsScreen />}
      </div>

      {activeBottomTab === 'home' && !isSidebarOpen && (
        <button onClick={() => setIsComposeOpen(true)} className="fixed right-6 bottom-28 w-16 h-16 bg-primary text-white rounded-[24px] shadow-2xl flex items-center justify-center active:scale-90 transition-all hover:rotate-12 z-40 border border-white/20"><Plus size={32} strokeWidth={3} /></button>
      )}

      {!isSidebarOpen && !activeChatId && !isSettingsOpen && !isUpdatesOpen && <BottomNav activeTab={activeBottomTab} onTabChange={setActiveBottomTab} />}
    </main>
  );
}
