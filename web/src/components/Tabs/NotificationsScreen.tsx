"use client";

import React, { useState, useEffect } from 'react';
import { db } from '@/lib/firebase';
import { collection, query, where, orderBy, onSnapshot, limit } from 'firebase/firestore';
import { Heart, MessageSquare, Bell, UserPlus } from 'lucide-react';

export const NotificationsScreen = ({ myUsername }: { myUsername: string }) => {
  const [notifications, setNotifications] = useState<any[]>([]);

  useEffect(() => {
    const q = query(
      collection(db, "notifications"),
      where("receiverId", "==", myUsername),
      orderBy("timestamp", "desc"),
      limit(30)
    );

    const unsubscribe = onSnapshot(q, (snapshot) => {
      setNotifications(snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() })));
    });

    return () => unsubscribe();
  }, [myUsername]);

  const getIcon = (type: string) => {
    switch (type) {
      case 'LIKE': return <Heart size={20} className="text-pink-500 fill-pink-500" />;
      case 'COMMENT': return <MessageSquare size={20} className="text-blue-500 fill-blue-500" />;
      case 'CHAT_MESSAGE': return <Bell size={20} className="text-primary fill-primary" />;
      default: return <UserPlus size={20} className="text-primary fill-primary" />;
    }
  };

  return (
    <div className="min-h-screen px-4 pt-4 animate-in fade-in duration-500 space-y-3 pb-32">
      <h2 className="text-2xl font-black uppercase tracking-tighter text-zinc-400 mb-6 ml-2">Уведомления</h2>

      {notifications.map((notif) => (
        <div
          key={notif.id}
          className="flex gap-4 p-5 glass rounded-[32px] hover:scale-[1.01] active:scale-[0.98] transition-all shadow-sm"
        >
          <div className="relative">
            <div className="w-14 h-14 rounded-[20px] overflow-hidden bg-zinc-100 dark:bg-zinc-800 flex-shrink-0">
                {notif.senderAvatarUrl ? (
                    <img src={notif.senderAvatarUrl} className="w-full h-full object-cover" alt="" />
                ) : (
                    <div className="w-full h-full flex items-center justify-center text-primary font-black text-xl">
                        {notif.senderName?.charAt(0).toUpperCase() || '?'}
                    </div>
                )}
            </div>
            <div className="absolute -bottom-1 -right-1 bg-white dark:bg-zinc-900 p-1.5 rounded-full shadow-lg scale-90 border border-zinc-500/10">
                {getIcon(notif.type)}
            </div>
          </div>

          <div className="flex-1 min-w-0">
            <p className="text-[15px] leading-snug">
                <span className="font-black text-zinc-900 dark:text-zinc-100">{notif.senderName}</span>
                <span className="text-zinc-500 font-medium"> {notif.type === 'LIKE' ? 'лайкнул ваш пост' : notif.type === 'COMMENT' ? 'ответил на ваш пост' : 'прислал сообщение'}</span>
            </p>
            {notif.text && (
                <p className="text-sm text-zinc-400 font-medium mt-1 truncate italic">"{notif.text}"</p>
            )}
            <p className="text-[10px] text-zinc-500 font-black uppercase mt-2 opacity-50">
                {notif.timestamp?.toDate().toLocaleDateString('ru-RU', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })}
            </p>
          </div>
        </div>
      ))}

      {notifications.length === 0 && (
        <div className="text-center py-40">
          <div className="text-7xl mb-8 grayscale opacity-10">🔔</div>
          <p className="text-zinc-400 font-black text-lg uppercase tracking-tighter italic">Пока что тихо...</p>
        </div>
      )}
    </div>
  );
};
