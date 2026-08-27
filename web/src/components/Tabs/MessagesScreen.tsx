"use client";

import React, { useState, useEffect } from 'react';
import { db } from '@/lib/firebase';
import { collection, query, where, orderBy, onSnapshot } from 'firebase/firestore';
import { MessageSquarePlus } from 'lucide-react';

export const MessagesScreen = ({ myUsername, onChatClick }: any) => {
  const [chats, setChats] = useState<any[]>([]);

  useEffect(() => {
    // В Android чаты обычно ищутся в 'chats' где в массиве 'participants' есть текущий пользователь
    const q = query(
      collection(db, "chats"),
      where("participants", "array-contains", myUsername),
      orderBy("lastMessageTimestamp", "desc")
    );

    return onSnapshot(q, (snapshot) => {
      setChats(snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() })));
    });
  }, [myUsername]);

  return (
    <div className="min-h-screen px-4 pt-4 pb-32 space-y-2">
      <div className="flex items-center justify-between px-2 mb-6">
        <h2 className="text-2xl font-black uppercase tracking-tighter text-zinc-400">Сообщения</h2>
        <MessageSquarePlus size={28} className="text-zinc-400" />
      </div>

      {chats.map((chat) => {
        const peer = chat.participants.find((p: string) => p !== myUsername) || myUsername;
        return (
          <div key={chat.id} onClick={() => onChatClick(chat.id)} className="flex items-center gap-4 p-4 hover:bg-primary/5 rounded-[28px] cursor-pointer transition-all border-b border-zinc-500/5">
            <div className="w-14 h-14 rounded-full bg-primary/10 flex-shrink-0 flex items-center justify-center font-black text-primary text-xl uppercase border-2 border-white dark:border-zinc-800">
                {peer.charAt(0)}
            </div>
            <div className="flex-1 min-w-0">
                <div className="flex justify-between items-center">
                    <span className="font-black text-zinc-800 dark:text-zinc-100 truncate">@{peer}</span>
                    <span className="text-[10px] font-bold text-zinc-400">{chat.lastMessageTimestamp ? new Date(chat.lastMessageTimestamp).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'}) : ''}</span>
                </div>
                <p className="text-sm text-zinc-500 truncate font-medium">{chat.lastMessage || 'Нет сообщений'}</p>
            </div>
          </div>
        );
      })}
    </div>
  );
};
