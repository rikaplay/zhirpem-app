"use client";

import React, { useEffect, useState, useRef } from 'react';
import { db } from '@/lib/firebase';
import { doc, onSnapshot, collection, query, where, orderBy, limit, getDocs, startAfter, updateDoc, arrayUnion, arrayRemove, serverTimestamp, setDoc, deleteDoc, addDoc } from 'firebase/firestore';
import { ArrowLeft, MoreVertical, Edit2, Verified, Star, Loader2, Image as ImageIcon, Palette } from 'lucide-react';
import { PostItem } from '../Feed/PostItem';

export const UserProfileScreen = ({ username, myUser, onBack }: any) => {
  const [user, setUser] = useState<any>(null);
  const [posts, setPosts] = useState<any[]>([]);
  const [reposts, setReposts] = useState<any[]>([]);
  const [activeTab, setActiveTab] = useState('posts');
  const [loading, setLoading] = useState(true);
  const [lastPost, setLastPost] = useState<any>(null);
  const [lastRepost, setLastPostRepost] = useState<any>(null);
  const [hasMore, setHasMore] = useState(true);
  const [isFollowing, setIsFollowing] = useState(false);
  const [isEditing, setIsEditing] = useState(false);

  // Customization states
  const [editData, setEditData] = useState<any>({});
  const fileInputRef = useRef<HTMLInputElement>(null);

  const cleanUsername = username.replace('@', '');

  useEffect(() => {
    // 1. Live Profile Data
    const unsubUser = onSnapshot(doc(db, "users", cleanUsername), (snap) => {
      if (snap.exists()) {
        const data = snap.data();
        setUser({ id: snap.id, ...data });
        setEditData(data);
      }
      setLoading(false);
    });

    // 2. Check Following
    const qF = query(collection(db, "follows"), where("follower", "==", myUser.id), where("following", "==", cleanUsername));
    const unsubFollow = onSnapshot(qF, (snap) => setIsFollowing(!snap.empty));

    // 3. Initial 5 Posts
    loadMorePosts(true);

    return () => { unsubUser(); unsubFollow(); };
  }, [username]);

  const loadMorePosts = async (isFirst = false) => {
    const q = query(
      collection(db, "zhirpem_posts"),
      where("handle", "==", `@${cleanUsername}`),
      orderBy("timestamp", "desc"),
      isFirst ? limit(5) : startAfter(lastPost),
      limit(5)
    );
    const snap = await getDocs(q);
    const newPosts = snap.docs.map(d => ({ id: d.id, ...d.data() }));
    setPosts(prev => isFirst ? newPosts : [...prev, ...newPosts]);
    if (snap.docs.length > 0) setLastPost(snap.docs[snap.docs.length - 1]);
    setHasMore(snap.docs.length === 5);
  };

  const handleFollow = async () => {
    const ref = collection(db, "follows");
    if (isFollowing) {
        const q = query(ref, where("follower", "==", myUser.id), where("following", "==", cleanUsername));
        const snap = await getDocs(q);
        snap.forEach(d => deleteDoc(d.ref));
    } else {
        await addDoc(ref, { follower: myUser.id, following: cleanUsername, timestamp: serverTimestamp() });
    }
    if (window.navigator.vibrate) window.navigator.vibrate(10);
  };

  const saveProfile = async () => {
    await updateDoc(doc(db, "users", cleanUsername), editData);
    setIsEditing(false);
    if (window.navigator.vibrate) window.navigator.vibrate(20);
  };

  if (loading && !user) return <div className="flex h-screen items-center justify-center"><Loader2 className="animate-spin text-primary" /></div>;

  return (
    <div className="fixed inset-0 z-50 bg-background-light dark:bg-background-dark overflow-y-auto pb-24">
      {/* Header */}
      <div className="sticky top-0 z-10 bg-white/80 dark:bg-zinc-900/80 backdrop-blur-md px-4 py-3 flex items-center justify-between">
        <div className="flex items-center gap-6">
          <button onClick={onBack} className="p-1 hover:bg-zinc-100 rounded-full"><ArrowLeft size={24} /></button>
          <div>
            <h1 className="font-black text-[17px] uppercase tracking-tight truncate max-w-[200px]">{user.name}</h1>
            <p className="text-[10px] font-black text-zinc-400 uppercase">{posts.length} записей</p>
          </div>
        </div>
        <button className="p-2 text-zinc-400"><MoreVertical size={20} /></button>
      </div>

      <div className="relative">
        <div className="w-full h-[160px]" style={{ backgroundColor: user.bannerColor || '#4A61FF' }}>
            {user.bannerUrl && <img src={user.bannerUrl} className="w-full h-full object-cover" />}
        </div>

        <div className="px-5">
            <div className="relative -mt-16 mb-4 flex justify-between items-end">
                <div className="relative">
                    <div className="w-[110px] h-[110px] rounded-full border-[6px] border-background-light dark:border-background-dark overflow-hidden bg-zinc-200 shadow-xl">
                        <img src={user.avatarUrl || '/placeholder.png'} className="w-full h-full object-cover" />
                    </div>
                    <div className="absolute bottom-2.5 right-2.5 w-5 h-5 bg-green-500 border-[3px] border-background-light dark:border-background-dark rounded-full shadow-lg" />
                </div>

                {cleanUsername === myUser.id ? (
                    <button onClick={() => setIsEditing(true)} className="mb-2 bg-primary text-white px-6 py-2 rounded-full font-black text-sm uppercase tracking-widest shadow-lg active:scale-95 transition-all">Изм. профиль</button>
                ) : (
                    <button onClick={handleFollow} className={`mb-2 px-6 py-2 rounded-full font-black text-sm uppercase tracking-widest shadow-lg active:scale-95 transition-all ${isFollowing ? 'bg-zinc-200 text-zinc-500' : 'bg-primary text-white'}`}>
                        {isFollowing ? 'Читаю' : 'Читать'}
                    </button>
                )}
            </div>

            <div className="flex items-center gap-1.5 mb-0.5">
                <h2 className="text-2xl font-black text-zinc-900 dark:text-white uppercase tracking-tighter" style={{ color: user.nameColor }}>{user.name}</h2>
                {user.blueBadge && <div className="w-5 h-5 bg-blue-500 rounded-full flex items-center justify-center p-0.5"><Verified size={14} className="text-white" /></div>}
            </div>
            <p className="text-zinc-400 font-bold text-lg mb-1">@{user.username || user.id}</p>
            <p className="text-primary font-black text-[11px] mb-4 uppercase tracking-[0.2em]">В сети</p>

            {user.status && <p className="text-primary font-black text-[15px] mb-4">{user.status}</p>}

            <div className="bg-[#E2DFE9]/30 dark:bg-zinc-800/40 p-5 rounded-[28px] mb-6 font-medium text-[15px] border border-white/10">
                {user.bio || "Пользователь Жирпема 🚀"}
            </div>

            <div className="flex gap-2 mb-8">
                <div className="flex-1 glass p-4 rounded-[24px] text-center shadow-sm">
                    <p className="text-primary font-black text-xl leading-none mb-1">{user.followingCount || 0}</p>
                    <p className="text-[10px] font-black text-zinc-400 uppercase tracking-widest">подписок</p>
                </div>
                <div className="flex-1 glass p-4 rounded-[24px] text-center shadow-sm">
                    <p className="text-primary font-black text-xl leading-none mb-1">{user.followersCount || 0}</p>
                    <p className="text-[10px] font-black text-zinc-400 uppercase tracking-widest">подписчиков</p>
                </div>
            </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex border-b border-zinc-500/10 px-4 sticky top-[60px] bg-background-light/90 dark:bg-background-dark/90 backdrop-blur-md z-10">
        {['Записи', 'Репосты'].map(tab => (
            <button key={tab} className={`flex-1 py-4 font-black uppercase text-[12px] tracking-widest ${activeTab === (tab === 'Записи' ? 'posts' : 'reposts') ? 'text-primary' : 'text-zinc-400'}`}>
                {tab}{activeTab === (tab === 'Записи' ? 'posts' : 'reposts') && <div className="absolute bottom-0 left-6 right-6 h-1 bg-primary rounded-t-full" />}
            </button>
        ))}
      </div>

      <div className="p-4 space-y-4 max-w-[500px] mx-auto">
        {posts.map(post => <PostItem key={post.id} post={post} myUsername={myUser.id} myUser={myUser} onUserClick={() => {}} onHashtagClick={() => {}} />)}
        {hasMore && <button onClick={() => loadMorePosts()} className="w-full py-4 text-primary font-black uppercase text-xs tracking-widest">Загрузить еще</button>}
      </div>

      {/* Edit Dialog */}
      {isEditing && (
          <div className="fixed inset-0 z-[100] bg-black/60 backdrop-blur-md flex flex-col p-6 animate-in slide-in-from-bottom duration-300">
              <div className="flex items-center justify-between mb-8">
                  <button onClick={() => setIsEditing(false)}><X size={28} /></button>
                  <button onClick={saveProfile} className="bg-primary text-white px-8 py-2 rounded-full font-black uppercase text-sm tracking-widest">Сохранить</button>
              </div>
              <div className="space-y-6 overflow-y-auto">
                  <div><label className="text-[10px] font-black uppercase text-zinc-400 ml-4 mb-1 block">Имя</label><input className="w-full bg-zinc-100 dark:bg-zinc-800 p-4 rounded-2xl outline-none font-bold" value={editData.name} onChange={e => setEditData({...editData, name: e.target.value})} /></div>
                  <div><label className="text-[10px] font-black uppercase text-zinc-400 ml-4 mb-1 block">Статус</label><input className="w-full bg-zinc-100 dark:bg-zinc-800 p-4 rounded-2xl outline-none font-bold" value={editData.status} onChange={e => setEditData({...editData, status: e.target.value})} /></div>
                  <div><label className="text-[10px] font-black uppercase text-zinc-400 ml-4 mb-1 block">HEX Цвет ника</label><input className="w-full bg-zinc-100 dark:bg-zinc-800 p-4 rounded-2xl outline-none font-bold" value={editData.nameColor} onChange={e => setEditData({...editData, nameColor: e.target.value})} placeholder="#006B44" /></div>
                  <div><label className="text-[10px] font-black uppercase text-zinc-400 ml-4 mb-1 block">Описание</label><textarea className="w-full bg-zinc-100 dark:bg-zinc-800 p-4 rounded-2xl outline-none font-bold h-32" value={editData.bio} onChange={e => setEditData({...editData, bio: e.target.value})} /></div>
              </div>
          </div>
      )}
    </div>
  );
};
