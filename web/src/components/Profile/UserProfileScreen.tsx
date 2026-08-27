"use client";

import React, { useEffect, useState } from 'react';
import { db } from '@/lib/firebase';
import { doc, getDoc, collection, query, where, orderBy, getDocs, limit } from 'firebase/firestore';
import { ArrowLeft, MoreVertical, Edit2, Verified, Star } from 'lucide-react';
import { PostItem } from '../Feed/PostItem';

interface UserProfileProps {
  username: string;
  myUser: any;
  onBack: () => void;
}

export const UserProfileScreen: React.FC<UserProfileProps> = ({ username, myUser, onBack }) => {
  const [user, setUser] = useState<any>(null);
  const [posts, setPosts] = useState<any[]>([]);
  const [activeTab, setActiveTab] = useState('posts');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadProfile = async () => {
      const userDoc = await getDoc(doc(db, "users", username));
      if (userDoc.exists()) {
        setUser({ id: userDoc.id, ...userDoc.data() });
      }

      const q = query(
        collection(db, "zhirpem_posts"),
        where("handle", "==", `@${username}`),
        orderBy("timestamp", "desc"),
        limit(20)
      );
      const snap = await getDocs(q);
      setPosts(snap.docs.map(d => ({ id: d.id, ...d.data() })));
      setLoading(false);
    };
    loadProfile();
  }, [username]);

  if (loading || !user) return <div className="flex h-screen items-center justify-center"><div className="animate-spin rounded-full h-8 w-8 border-4 border-primary border-t-transparent" /></div>;

  return (
    <div className="fixed inset-0 z-50 bg-background-light dark:bg-background-dark overflow-y-auto pb-20">
      {/* Header */}
      <div className="sticky top-0 z-10 bg-white/80 dark:bg-zinc-900/80 backdrop-blur-md px-4 py-3 flex items-center justify-between">
        <div className="flex items-center gap-6">
          <button onClick={onBack} className="p-1 hover:bg-zinc-100 dark:hover:bg-zinc-800 rounded-full"><ArrowLeft size={24} /></button>
          <div>
            <h1 className="font-black text-[17px] uppercase tracking-tight leading-tight">{user.name}</h1>
            <p className="text-[12px] font-bold text-zinc-400 uppercase tracking-tighter">{posts.length} записей</p>
          </div>
        </div>
        <button className="p-2 text-zinc-400"><MoreVertical size={20} /></button>
      </div>

      {/* Banner & Avatar Area */}
      <div className="relative">
        <div
          className="w-full h-[150px]"
          style={{ backgroundColor: user.bannerColor || '#4A61FF' }}
        >
          {user.bannerUrl && <img src={user.bannerUrl} className="w-full h-full object-cover" />}
        </div>

        <div className="px-5">
            <div className="relative -mt-12 mb-3 inline-block">
                <div className="w-[100px] h-[100px] rounded-full border-4 border-background-light dark:border-background-dark overflow-hidden bg-zinc-200">
                    <img src={user.avatarUrl || '/placeholder.png'} className="w-full h-full object-cover" />
                </div>
                <div className="absolute bottom-1 right-1 w-6 h-6 bg-green-500 border-4 border-background-light dark:border-background-dark rounded-full shadow-sm" />
                {username === myUser.id && (
                    <button className="absolute -bottom-1 -right-4 p-2 bg-primary text-white rounded-full shadow-lg border-2 border-white dark:border-zinc-900">
                        <Edit2 size={14} strokeWidth={3} />
                    </button>
                )}
            </div>

            <div className="flex items-center gap-1.5 mb-0.5">
                <h2 className="text-2xl font-black text-zinc-900 dark:text-white uppercase tracking-tighter">{user.name}</h2>
                {user.blueBadge && <Verified size={20} className="text-blue-500 fill-blue-500" />}
                {user.yellowBadge && <Star size={20} className="text-yellow-500 fill-yellow-500" />}
            </div>
            <p className="text-zinc-400 font-bold text-lg mb-2">@{user.username || user.id}</p>
            <p className="text-primary font-bold text-sm mb-4">В сети</p>

            {/* Bio */}
            {user.bio && (
                <div className="bg-zinc-100 dark:bg-zinc-800/50 p-4 rounded-[24px] mb-6 font-medium text-[15px]">
                    {user.bio}
                </div>
            )}

            {/* Stats */}
            <div className="flex gap-1.5 mb-6">
                <div className="flex-1 bg-zinc-50 dark:bg-zinc-900/30 p-4 rounded-[20px] text-center border border-zinc-500/5">
                    <p className="text-primary font-black text-xl leading-none mb-1">6</p>
                    <p className="text-[11px] font-bold text-zinc-400 uppercase tracking-widest">подписок</p>
                </div>
                <div className="flex-1 bg-zinc-50 dark:bg-zinc-900/30 p-4 rounded-[20px] text-center border border-zinc-500/5">
                    <p className="text-primary font-black text-xl leading-none mb-1">6</p>
                    <p className="text-[11px] font-bold text-zinc-400 uppercase tracking-widest">подписчиков</p>
                </div>
            </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex border-b border-zinc-500/10 px-4">
        {['Записи', 'Репосты'].map((tab, i) => {
            const id = i === 0 ? 'posts' : 'reposts';
            const isActive = activeTab === id;
            return (
                <button
                    key={id}
                    onClick={() => setActiveTab(id)}
                    className={`flex-1 py-4 font-black uppercase text-[13px] tracking-widest transition-all relative ${isActive ? 'text-primary' : 'text-zinc-400'}`}
                >
                    {tab}
                    {isActive && <div className="absolute bottom-0 left-4 right-4 h-1 bg-primary rounded-t-full" />}
                </button>
            )
        })}
      </div>

      {/* Content */}
      <div className="p-4 space-y-4">
        {posts.map(post => (
            <PostItem key={post.id} post={post} myUsername={myUser.id} myUser={myUser} onUserClick={() => {}} onHashtagClick={() => {}} />
        ))}
      </div>
    </div>
  );
};
