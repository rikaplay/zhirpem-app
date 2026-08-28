"use client";

import React, { useEffect, useState, useMemo } from 'react';
import { db } from '@/lib/firebase';
import { doc, getDoc, collection, query, where, orderBy, onSnapshot, limit } from 'firebase/firestore';
import { ArrowLeft, MoreVertical, Edit2, Verified, Star, Loader2 } from 'lucide-react';
import { PostItem } from '../Feed/PostItem';

interface UserProfileProps {
  username: string;
  myUser: any;
  onBack: () => void;
}

export const UserProfileScreen: React.FC<UserProfileProps> = ({ username, myUser, onBack }) => {
  const [user, setUser] = useState<any>(null);
  const [posts, setPosts] = useState<any[]>([]);
  const [reposts, setReposts] = useState<any[]>([]);
  const [activeTab, setActiveTab] = useState('posts');
  const [loading, setLoading] = useState(true);

  // 1. Оптимизированная загрузка профиля и постов через Snapshot
  useEffect(() => {
    setLoading(true);
    const cleanUsername = username.replace('@', '');

    // Загрузка данных пользователя
    const userUnsub = onSnapshot(doc(db, "users", cleanUsername), (docSnap) => {
      if (docSnap.exists()) {
        setUser({ id: docSnap.id, ...docSnap.data() });
      }
      setLoading(false);
    });

    // Загрузка постов автора
    const postsQuery = query(
      collection(db, "zhirpem_posts"),
      where("handle", "==", `@${cleanUsername}`),
      orderBy("timestamp", "desc"),
      limit(20)
    );
    const postsUnsub = onSnapshot(postsQuery, (snap) => {
      setPosts(snap.docs.map(d => ({ id: d.id, ...d.data() })));
    });

    // Загрузка репостов (если в базе есть такое поле)
    const repostsQuery = query(
        collection(db, "zhirpem_posts"),
        where("repostedBy", "array-contains", cleanUsername),
        orderBy("timestamp", "desc"),
        limit(20)
      );
    const repostsUnsub = onSnapshot(repostsQuery, (snap) => {
        setReposts(snap.docs.map(d => ({ id: d.id, ...d.data() })));
    });

    return () => {
      userUnsub();
      postsUnsub();
      repostsUnsub();
    };
  }, [username]);

  const displayedPosts = activeTab === 'posts' ? posts : reposts;

  if (loading && !user) return (
    <div className="fixed inset-0 z-50 bg-background-light dark:bg-background-dark flex flex-col items-center justify-center">
        <Loader2 className="animate-spin text-primary mb-4" size={40} />
        <p className="font-black uppercase tracking-widest text-xs opacity-40">Загрузка Жирпема...</p>
    </div>
  );

  if (!user) return <div className="p-20 text-center">Пользователь не найден</div>;

  return (
    <div className="fixed inset-0 z-50 bg-background-light dark:bg-background-dark overflow-y-auto pb-20 animate-in fade-in slide-in-from-right duration-300">
      {/* Header */}
      <div className="sticky top-0 z-10 bg-white/80 dark:bg-zinc-900/80 backdrop-blur-md px-4 py-3 flex items-center justify-between border-b border-zinc-500/5">
        <div className="flex items-center gap-6">
          <button onClick={onBack} className="p-1 hover:bg-zinc-100 dark:hover:bg-zinc-800 rounded-full transition-transform active:scale-75"><ArrowLeft size={24} /></button>
          <div className="min-w-0">
            <h1 className="font-black text-[17px] uppercase tracking-tight leading-tight truncate">{user.name}</h1>
            <p className="text-[10px] font-black text-zinc-400 uppercase tracking-tighter">{posts.length} записей</p>
          </div>
        </div>
        <button className="p-2 text-zinc-400"><MoreVertical size={20} /></button>
      </div>

      {/* Banner & Avatar Area */}
      <div className="relative">
        <div
          className="w-full h-[160px] bg-zinc-200 dark:bg-zinc-800 transition-colors"
          style={{ backgroundColor: user.bannerUrl ? 'transparent' : (user.bannerColor || '#4A61FF') }}
        >
          {user.bannerUrl && <img src={user.bannerUrl} className="w-full h-full object-cover" alt="" />}
        </div>

        <div className="px-5">
            <div className="relative -mt-16 mb-4 inline-block group">
                <div className="w-[110px] h-[110px] rounded-full border-[6px] border-background-light dark:border-background-dark overflow-hidden bg-zinc-200 shadow-xl ring-1 ring-black/5">
                    <img src={user.avatarUrl || '/placeholder.png'} className="w-full h-full object-cover" alt="" />
                </div>
                <div className="absolute bottom-2.5 right-2.5 w-5 h-5 bg-green-500 border-[3px] border-background-light dark:border-background-dark rounded-full shadow-lg" title="В сети" />

                {user.joinedCommunityAvatar && (
                    <div className="absolute -bottom-1 -left-1 w-9 h-9 rounded-full border-[3px] border-background-light dark:border-background-dark overflow-hidden bg-white shadow-lg">
                        <img src={user.joinedCommunityAvatar} className="w-full h-full object-cover" alt="" />
                    </div>
                )}
            </div>

            <div className="flex items-center gap-1.5 mb-0.5">
                <h2 className="text-2xl font-black text-zinc-900 dark:text-white uppercase tracking-tighter" style={{ color: user.nameColor || 'inherit' }}>
                    {user.name}
                </h2>
                <div className="flex items-center gap-0.5">
                    {user.blueBadge && <Verified size={18} className="text-blue-500 fill-blue-500" />}
                    {user.yellowBadge && <Star size={18} className="text-yellow-500 fill-yellow-500" />}
                </div>
            </div>
            <p className="text-zinc-400 font-bold text-[17px] mb-1">@{user.username || user.id}</p>
            <p className="text-primary font-black text-[11px] mb-5 uppercase tracking-[0.2em] opacity-80">В сети</p>

            {/* Custom Status */}
            {user.status && (
                <div className="mb-4 inline-block bg-primary/5 px-4 py-1.5 rounded-full border border-primary/10">
                    <p className="text-primary font-black text-sm">{user.status}</p>
                </div>
            )}

            {/* Bio */}
            <div className="bg-[#E2DFE9]/30 dark:bg-zinc-800/40 p-5 rounded-[28px] mb-6 font-medium text-[15px] leading-relaxed text-zinc-700 dark:text-zinc-300 border border-white/10 shadow-inner">
                {user.bio || "Пользователь Жирпема 🚀"}
            </div>

            {/* Stats */}
            <div className="flex gap-2 mb-8">
                <div className="flex-1 glass p-4 rounded-[24px] text-center shadow-sm active:scale-95 transition-transform cursor-pointer">
                    <p className="text-primary font-black text-xl leading-none mb-1">{user.followingCount || 0}</p>
                    <p className="text-[10px] font-black text-zinc-400 uppercase tracking-[0.1em]">подписок</p>
                </div>
                <div className="flex-1 glass p-4 rounded-[24px] text-center shadow-sm active:scale-95 transition-transform cursor-pointer">
                    <p className="text-primary font-black text-xl leading-none mb-1">{user.followersCount || 0}</p>
                    <p className="text-[10px] font-black text-zinc-400 uppercase tracking-[0.1em]">подписчиков</p>
                </div>
            </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex border-b border-zinc-500/10 px-4 sticky top-[60px] bg-background-light/80 dark:bg-background-dark/80 backdrop-blur-md z-10">
        {[
            { id: 'posts', label: 'Записи' },
            { id: 'reposts', label: 'Репосты' }
        ].map((tab) => {
            const isActive = activeTab === tab.id;
            return (
                <button
                    key={tab.id}
                    onClick={() => setActiveTab(tab.id)}
                    className={`flex-1 py-4 font-black uppercase text-[12px] tracking-[0.2em] transition-all relative ${isActive ? 'text-primary' : 'text-zinc-400 hover:text-zinc-600'}`}
                >
                    {tab.label}
                    {isActive && <div className="absolute bottom-0 left-6 right-6 h-1 bg-primary rounded-t-full shadow-[0_-2px_10px_rgba(0,107,68,0.3)]" />}
                </button>
            )
        })}
      </div>

      {/* Content Feed */}
      <div className="p-4 space-y-4 max-w-[500px] mx-auto">
        {displayedPosts.length > 0 ? (
            displayedPosts.map(post => (
                <PostItem key={post.id} post={post} myUsername={myUser.id} myUser={myUser} onUserClick={() => {}} onHashtagClick={() => {}} />
            ))
        ) : (
            <div className="text-center py-20 opacity-20 font-black uppercase tracking-widest text-xs italic">
                Здесь пока пусто
            </div>
        )}
      </div>
    </div>
  );
};
