"use client";

import React, { useEffect, useState } from 'react';
import { db } from '@/lib/firebase';
import {
    collection,
    query,
    where,
    orderBy,
    onSnapshot,
    addDoc,
    serverTimestamp,
    doc,
    updateDoc,
    increment
} from 'firebase/firestore';
import { Send, X } from 'lucide-react';

interface CommentsProps {
  postId: string;
  myUser: any;
}

export const Comments: React.FC<CommentsProps> = ({ postId, myUser }) => {
  const [comments, setComments] = useState<any[]>([]);
  const [text, setText] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const q = query(
      collection(db, "comments"),
      where("postId", "==", postId),
      orderBy("timestamp", "asc")
    );

    const unsubscribe = onSnapshot(q, (snapshot) => {
      setComments(snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() })));
    });

    return () => unsubscribe();
  }, [postId]);

  const sendComment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!text.trim() || loading) return;
    setLoading(true);

    try {
      await addDoc(collection(db, "comments"), {
        postId,
        author: myUser.name,
        authorUsername: `@${myUser.id}`,
        text: text.trim(),
        timestamp: serverTimestamp(),
        likesCount: 0,
        likedBy: []
      });

      await updateDoc(doc(db, "zhirpem_posts", postId), {
        commentsCount: increment(1)
      });

      setText('');
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="mt-4 pt-4 border-t border-zinc-500/10 space-y-4">
      <div className="space-y-3 max-h-[300px] overflow-y-auto pr-2 custom-scrollbar">
        {comments.map((comment) => (
          <div key={comment.id} className="flex gap-2 items-start">
            <div className="w-7 h-7 rounded-full bg-primary/10 flex-shrink-0 flex items-center justify-center text-[10px] font-bold text-primary border border-primary/20">
              {comment.author?.charAt(0).toUpperCase()}
            </div>
            <div className="flex-1 bg-zinc-100 dark:bg-zinc-800/50 rounded-2xl px-3 py-2">
              <div className="flex items-center gap-2 mb-0.5">
                <span className="font-bold text-xs">{comment.author}</span>
                <span className="text-[10px] text-zinc-500">{comment.authorUsername}</span>
              </div>
              <p className="text-sm leading-snug">{comment.text}</p>
            </div>
          </div>
        ))}
        {comments.length === 0 && (
          <p className="text-center text-xs text-zinc-500 py-2 italic">Будьте первым, кто ответит!</p>
        )}
      </div>

      <form onSubmit={sendComment} className="flex gap-2 items-center mt-2">
        <input
          type="text"
          placeholder="Ваш ответ..."
          className="flex-1 bg-zinc-100 dark:bg-zinc-800 border-none rounded-full px-4 py-2.5 text-sm outline-none focus:ring-1 focus:ring-primary/50"
          value={text}
          onChange={(e) => setText(e.target.value)}
        />
        <button
          type="submit"
          disabled={!text.trim() || loading}
          className="p-2.5 bg-primary text-white dark:text-zinc-900 rounded-full active:scale-90 transition-transform disabled:opacity-50"
        >
          <Send size={16} strokeWidth={3} />
        </button>
      </form>
    </div>
  );
};
