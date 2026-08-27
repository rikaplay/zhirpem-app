"use client";

import React, { useState, useEffect } from 'react';
import { db } from '@/lib/firebase';
import { collection, query, where, orderBy, onSnapshot, limit } from 'firebase/firestore';
import { MessageCircle } from 'lucide-react';

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
    <div className="min-h-screen px-4 pt-4 animate-in fade-in duration-500 space-y-3 pb-32">
      <div className="flex items-center justify-between mb-6 ml-2">
        <h2 className="text-2xl font-black uppercase tracking-tighter text-zinc-400">Чаты</h2>
        <button className="bg-primary/10 text-primary p-3 rounded-[18px] hover:scale-110 active:scale-95 transition-all">
            <MessageCircle size={22} strokeWidth={3} />
        </button>
      </div>

      {chats.map((chat) => {
        const peerUsername = chat.participants.find((p: string) => p !== myUsername) || myUsername;
        return (
          <div
            key={chat.id}
            className="flex items-center gap-4 p-5 glass rounded-[36px] hover:scale-[1.01] active:scale-[0.98] transition-all cursor-pointer shadow-sm border-white/10"
          >
            <div className="w-[60px] h-[60px] rounded-full bg-primary/10 overflow-hidden border-2 border-white dark:border-zinc-800 flex-shrink-0">
                <div className="w-full h-full flex items-center justify-center text-primary font-black text-2xl uppercase">
                    {peerUsername.charAt(0)}
                </div>
            </div>

            <div className="flex-1 min-w-0">
                <div className="flex justify-between items-center mb-0.5">
                    <h3 className="font-black text-[17px] text-zinc-900 dark:text-zinc-100 truncate">@{peerUsername}</h3>
                    <span className="text-[10px] font-black text-zinc-400 uppercase opacity-50">
                        {new Date(chat.lastMessageTimestamp).toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' })}
                    </span>
                </div>
                <p className="text-[14px] text-zinc-500 font-medium truncate pr-4">
                    {chat.lastMessage || 'Напишите что-нибудь...'}
                </p>
            </div>
          </div>
        );
      })}

      {chats.length === 0 && (
        <div className="text-center py-40">
          <div className="text-7xl mb-8 grayscale opacity-10">💬</div>
          <p className="text-zinc-400 font-black text-lg uppercase tracking-tighter italic">Нет активных чатов</p>
          <button className="mt-6 bg-primary text-white dark:text-zinc-900 px-8 py-3 rounded-2xl font-black uppercase text-sm tracking-widest shadow-lg">Начать общение</button>
        </div>
      )}
    </div>
  );
};
