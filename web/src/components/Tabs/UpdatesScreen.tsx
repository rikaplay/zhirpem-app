"use client";

import React, { useEffect, useState } from 'react';
import { db } from '@/lib/firebase';
import { collection, query, orderBy, onSnapshot, limit, doc, updateDoc, increment, arrayUnion } from 'firebase/firestore';
import { ArrowLeft, Heart, Eye } from 'lucide-react';

export const UpdatesScreen = ({ onBack, myUser }: { onBack: () => void, myUser: any }) => {
  const [updates, setUpdates] = useState<any[]>([]);

  useEffect(() => {
    const q = query(collection(db, "update_news"), orderBy("timestamp", "desc"), limit(10));
    return onSnapshot(q, (snapshot) => {
      setUpdates(snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() })));
    });
  }, []);

  const handleLike = async (id: string, likedBy: string[]) => {
    const ref = doc(db, "update_news", id);
    const isLiked = likedBy?.includes(myUser.id);
    if (isLiked) return;

    await updateDoc(ref, {
      likes: increment(1),
      likedBy: arrayUnion(myUser.id)
    });
    if (window.navigator.vibrate) window.navigator.vibrate(10);
  };

  return (
    <div className="fixed inset-0 z-[70] bg-background-light dark:bg-background-dark flex flex-col animate-in slide-in-from-right duration-300 overflow-y-auto pb-20">
      <div className="p-6 flex items-center gap-6 sticky top-0 bg-background-light/90 dark:bg-background-dark/90 backdrop-blur-md z-10 border-b border-zinc-500/5">
        <button onClick={onBack} className="p-1 hover:bg-zinc-100 dark:hover:bg-zinc-800 rounded-full transition-transform active:scale-90"><ArrowLeft size={24} /></button>
        <h1 className="text-2xl font-black tracking-tight uppercase tracking-tighter">Новости</h1>
      </div>

      <div className="px-4 py-6 space-y-8">
        {updates.map((update) => (
          <div key={update.id} className="bg-white dark:bg-zinc-900 rounded-[32px] overflow-hidden shadow-sm border border-zinc-500/5">
            {update.imageUrl && (
                <img src={update.imageUrl} className="w-full h-52 object-cover" alt="" />
            )}
            <div className="p-6">
                <div className="flex items-center justify-between mb-4">
                    <span className="text-primary font-black text-xs uppercase tracking-widest bg-primary/10 px-3 py-1 rounded-full">v{update.version}</span>
                    <span className="text-[10px] font-bold text-zinc-400 uppercase">{update.timestamp?.toDate().toLocaleDateString()}</span>
                </div>
                <h3 className="text-xl font-black mb-3 text-zinc-800 dark:text-zinc-100">{update.title}</h3>
                <p className="text-zinc-500 dark:text-zinc-400 text-[15px] leading-relaxed mb-6 whitespace-pre-wrap">{update.text}</p>

                <div className="flex items-center gap-6 pt-4 border-t border-zinc-500/5">
                    <button
                        onClick={() => handleLike(update.id, update.likedBy)}
                        className={`flex items-center gap-2 font-black text-xs uppercase tracking-widest transition-all active:scale-90 ${update.likedBy?.includes(myUser.id) ? 'text-pink-500' : 'text-zinc-400'}`}
                    >
                        <Heart size={18} className={update.likedBy?.includes(myUser.id) ? 'fill-pink-500' : ''} />
                        <span>{update.likes || 0}</span>
                    </button>
                    <div className="flex items-center gap-2 text-zinc-400 font-black text-xs uppercase tracking-widest">
                        <Eye size={18} />
                        <span>{update.views || 0}</span>
                    </div>
                </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
