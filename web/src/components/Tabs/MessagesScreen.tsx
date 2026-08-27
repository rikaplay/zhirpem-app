"use client";

import React, { useState, useEffect } from 'react';
import { db } from '@/lib/firebase';
import { collection, query, where, orderBy, onSnapshot } from 'firebase/firestore';
import { MessageSquarePlus } from 'lucide-react';

export const MessagesScreen = ({ myUsername }: { myUsername: string }) => {
  const [chats, setChats] = useState<any[]>([]);

  useEffect(() => {
    const q = query(
      collection(db, "chats"),
      where("participants", "array-contains", myUsername),
      orderBy("lastMessageTimestamp", "desc")
    );

    const unsubscribe = onSnapshot(q, (snapshot) => {
      setChats(snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() })));
    });

    return () => unsubscribe();
  }, [myUsername]);

  return (
    <div className="min-h-screen bg-background-light dark:bg-background-dark px-4 pt-4 pb-32 animate-in fade-in duration-500">
      <div className="flex items-center justify-between px-2 mb-6">
        <h2 className="text-2xl font-black text-zinc-800 dark:text-white tracking-tight">Сообщения</h2>
        <button className="text-zinc-600 dark:text-zinc-400 p-2"><MessageSquarePlus size={28} /></button>
      </div>

      <div className="space-y-0.5">
        {chats.map((chat) => {
            const peerUsername = chat.participants.find((p: string) => p !== myUsername) || myUsername;
            return (
            <div
                key={chat.id}
                className="flex items-center gap-4 py-4 px-2 hover:bg-zinc-50 dark:hover:bg-zinc-800/50 rounded-2xl active:scale-[0.98] transition-all cursor-pointer border-b border-zinc-500/5"
            >
                <div className="w-[52px] h-[52px] rounded-full bg-primary/10 overflow-hidden border border-zinc-100 dark:border-zinc-800 flex-shrink-0">
                    <div className="w-full h-full flex items-center justify-center text-primary font-black text-xl uppercase">
                        {peerUsername.charAt(0)}
                    </div>
                </div>

                <div className="flex-1 min-w-0">
                    <div className="flex justify-between items-center mb-0.5">
                        <h3 className="font-bold text-[16px] text-zinc-900 dark:text-zinc-100 truncate">{peerUsername}</h3>
                        <span className="text-[11px] font-bold text-zinc-400 uppercase opacity-70">
                            {chat.lastMessageTimestamp ? new Date(chat.lastMessageTimestamp).toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' }) : ''}
                        </span>
                    </div>
                    <p className="text-[14px] text-zinc-500 font-medium truncate pr-4">
                        {chat.lastMessage || 'Нет сообщений'}
                    </p>
                </div>
            </div>
            );
        })}
      </div>

      {chats.length === 0 && (
        <div className="text-center py-40 opacity-20">
          <div className="text-7xl mb-4">💬</div>
          <p className="font-black uppercase tracking-widest text-sm">Чаты не найдены</p>
        </div>
      )}
    </div>
  );
};
