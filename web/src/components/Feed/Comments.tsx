"use client";

import React, { useEffect, useState } from 'react';
import { db } from '@/lib/firebase';
import { collection, query, where, orderBy, onSnapshot, addDoc, serverTimestamp, doc, updateDoc, increment } from 'firebase/firestore';
import { Send, X, CornerDownRight } from 'lucide-react';

export const Comments = ({ postId, myUser }: any) => {
  const [comments, setComments] = useState<any[]>([]);
  const [text, setText] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const q = query(collection(db, "comments"), where("postId", "==", postId), orderBy("timestamp", "asc"));
    return onSnapshot(q, (snapshot) => {
      setComments(snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() })));
    });
  }, [postId]);

  const send = async (e: any) => {
    e.preventDefault();
    if (!text.trim()) return;
    setLoading(true);
    await addDoc(collection(db, "comments"), {
      postId, author: myUser.name, authorUsername: `@${myUser.id}`,
      text: text.trim(), timestamp: serverTimestamp(), likesCount: 0
    });
    await updateDoc(doc(db, "zhirpem_posts", postId), { commentsCount: increment(1) });
    setText(''); setLoading(false);
  };

  return (
    <div className="mt-4 space-y-4">
      {comments.map((comment) => (
        <div key={comment.id} className="relative flex gap-3 pl-2">
          {/* Vertical Line */}
          <div className="absolute left-5 top-8 bottom-0 w-0.5 bg-zinc-100 dark:bg-zinc-800" />

          <div className="w-8 h-8 rounded-full bg-primary/10 flex-shrink-0 flex items-center justify-center text-[10px] font-bold text-primary z-10 border-2 border-white dark:border-zinc-900">
            {comment.author?.charAt(0).toUpperCase()}
          </div>
          <div className="flex-1 bg-zinc-50 dark:bg-zinc-800/40 rounded-[20px] px-4 py-2.5 shadow-sm">
            <div className="flex items-center gap-2 mb-0.5">
                <span className="font-black text-xs">{comment.author}</span>
                <span className="text-[10px] font-bold text-zinc-400 uppercase tracking-tighter">{comment.authorUsername}</span>
            </div>
            <p className="text-[14px] leading-snug">{comment.text}</p>
          </div>
        </div>
      ))}
      <form onSubmit={send} className="flex gap-2 items-center pt-2">
        <input
            type="text" placeholder="Ваш ответ..." value={text} onChange={e => setText(e.target.value)}
            className="flex-1 bg-zinc-100 dark:bg-zinc-800 rounded-full px-4 py-3 text-sm outline-none"
        />
        <button type="submit" className="p-3 bg-primary text-white rounded-full active:scale-90 transition-all"><Send size={18} /></button>
      </form>
    </div>
  );
};
