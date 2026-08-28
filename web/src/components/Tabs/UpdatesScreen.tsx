"use client";

import React, { useEffect, useState } from 'react';
import { db } from '@/lib/firebase';
import { collection, query, orderBy, onSnapshot, limit } from 'firebase/firestore';
import { ArrowLeft, Sparkles, Zap, Shield, Bug } from 'lucide-react';

export const UpdatesScreen = ({ onBack }: { onBack: () => void }) => {
  const [updates, setUpdates] = useState<any[]>([]);

  useEffect(() => {
    const q = query(collection(db, "update_news"), orderBy("timestamp", "desc"), limit(10));
    return onSnapshot(q, (snapshot) => {
      setUpdates(snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() })));
    });
  }, []);

  return (
    <div className="fixed inset-0 z-[70] bg-background-light dark:bg-background-dark flex flex-col animate-in slide-in-from-right duration-300 overflow-y-auto pb-20">
      <div className="p-6 flex items-center gap-6 sticky top-0 bg-background-light/90 dark:bg-background-dark/90 backdrop-blur-md z-10">
        <button onClick={onBack} className="p-1 hover:bg-zinc-100 dark:hover:bg-zinc-800 rounded-full transition-transform active:scale-90"><ArrowLeft size={24} /></button>
        <h1 className="text-2xl font-black tracking-tight uppercase tracking-tighter">Что нового</h1>
      </div>

      <div className="px-4 space-y-6">
        {updates.map((update) => (
          <div key={update.id} className="glass p-6 rounded-[32px] border-white/10 shadow-sm">
            <div className="flex items-center justify-between mb-4">
                <span className="bg-primary/10 text-primary px-4 py-1 rounded-full text-xs font-black uppercase tracking-widest border border-primary/20">v{update.version}</span>
                <span className="text-[10px] font-bold text-zinc-400 uppercase tracking-tighter">{update.timestamp?.toDate().toLocaleDateString()}</span>
            </div>
            <h3 className="text-xl font-black mb-3 text-zinc-800 dark:text-zinc-100">{update.title}</h3>
            <p className="text-zinc-500 dark:text-zinc-400 text-sm leading-relaxed mb-6 whitespace-pre-wrap">{update.text}</p>

            <div className="grid grid-cols-2 gap-3">
                {update.features?.map((f: string, i: number) => (
                    <div key={i} className="bg-zinc-50 dark:bg-zinc-800/40 p-3 rounded-2xl flex items-center gap-2">
                        <Sparkles size={14} className="text-primary" />
                        <span className="text-[11px] font-bold uppercase truncate">{f}</span>
                    </div>
                ))}
            </div>
          </div>
        ))}

        {updates.length === 0 && (
            <div className="text-center py-40 opacity-20">
                <div className="text-6xl mb-4">🚀</div>
                <p className="font-black uppercase tracking-widest text-sm">Новостей пока нет</p>
            </div>
        )}
      </div>
    </div>
  );
};
