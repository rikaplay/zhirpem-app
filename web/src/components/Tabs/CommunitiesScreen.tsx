"use client";

import React, { useState, useEffect } from 'react';
import { db } from '@/lib/firebase';
import { collection, onSnapshot } from 'firebase/firestore';

export const CommunitiesScreen = () => {
  const [comms, setComms] = useState<any[]>([]);

  useEffect(() => {
    return onSnapshot(collection(db, "communities"), (snap) => {
      setComms(snap.docs.map(doc => ({ id: doc.id, ...doc.data() })));
    });
  }, []);

  return (
    <div className="min-h-screen px-4 pt-4 pb-32 space-y-4">
      <h2 className="text-2xl font-black uppercase tracking-tighter text-zinc-400 mb-6">Сообщества 👥</h2>
      <div className="grid grid-cols-2 gap-4">
        {comms.map(c => (
            <div key={c.id} className="glass p-4 rounded-[28px] text-center border-white/10 shadow-sm active:scale-95 transition-transform cursor-pointer">
                <div className="w-16 h-16 rounded-full bg-primary/10 mx-auto mb-3 overflow-hidden border-2 border-white">
                    {c.avatarUrl ? <img src={c.avatarUrl} className="w-full h-full object-cover" /> : <span className="flex h-full items-center justify-center font-black text-primary">👥</span>}
                </div>
                <h3 className="font-black text-sm truncate">{c.name}</h3>
                <p className="text-[10px] font-bold text-zinc-500 uppercase mt-1">Вступить</p>
            </div>
        ))}
      </div>
    </div>
  );
};
