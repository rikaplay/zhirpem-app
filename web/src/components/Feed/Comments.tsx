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
import { Send, X, CornerDownRight } from 'lucide-react';

interface CommentsProps {
  postId: string;
  myUser: any;
}

export const Comments: React.FC<CommentsProps> = ({ postId, myUser }) => {
  const [comments, setComments] = useState<any[]>([]);
  const [text, setText] = useState('');
  const [replyTo, setReplyTo] = useState<any>(null);
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
        likedBy: [],
        replyToCommentId: replyTo?.id || null,
        replyToUsername: replyTo?.authorUsername || null
      });

      await updateDoc(doc(db, "zhirpem_posts", postId), {
        commentsCount: increment(1)
      });

      setText('');
      setReplyTo(null);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="mt-4 pt-4 border-t border-zinc-500/10 space-y-4">
      <div className="space-y-3 max-h-[400px] overflow-y-auto pr-2 custom-scrollbar">
        {comments.map((comment) => {
          const isReply = !!comment.replyToCommentId;
          return (
            <div
              key={comment.id}
              className={`flex gap-2 items-start group ${isReply ? 'ml-6' : ''}`}
            >
              {isReply && <CornerDownRight size={14} className="text-zinc-400 mt-2 flex-shrink-0" />}
              <div
                className={`${isReply ? 'w-6 h-6' : 'w-8 h-8'} rounded-full bg-primary/10 flex-shrink-0 flex items-center justify-center text-[10px] font-bold text-primary border border-primary/20`}
              >
                {comment.author?.charAt(0).toUpperCase()}
              </div>
              <div
                className="flex-1 bg-zinc-100 dark:bg-zinc-800/50 rounded-2xl px-3 py-2 cursor-pointer hover:bg-zinc-200 dark:hover:bg-zinc-800 transition-colors"
                onClick={() => setReplyTo(comment)}
              >
                <div className="flex items-center gap-2 mb-0.5">
                  <span className="font-bold text-xs">{comment.author}</span>
                  <span className="text-[10px] text-zinc-500">{comment.authorUsername}</span>
                </div>
                {comment.replyToUsername && (
                    <span className="text-[10px] text-primary font-bold block mb-1">
                        в ответ {comment.replyToUsername}
                    </span>
                )}
                <p className="text-sm leading-snug">{comment.text}</p>
              </div>
            </div>
          );
        })}
        {comments.length === 0 && (
          <p className="text-center text-xs text-zinc-500 py-2 italic">Будьте первым, кто ответит!</p>
        )}
      </div>

      <div className="flex flex-col gap-2">
        {replyTo && (
            <div className="flex items-center justify-between bg-primary/10 px-3 py-1.5 rounded-xl">
                <span className="text-[11px] font-bold text-primary">Ответ пользователю {replyTo.author}</span>
                <button onClick={() => setReplyTo(null)} className="text-primary hover:scale-110 transition-transform">
                    <X size={14} />
                </button>
            </div>
        )}
        <form onSubmit={sendComment} className="flex gap-2 items-center">
            <input
            type="text"
            placeholder={replyTo ? "Ваш ответ..." : "Написать комментарий..."}
            className="flex-1 bg-zinc-100 dark:bg-zinc-800 border-none rounded-full px-4 py-3 text-sm outline-none focus:ring-2 focus:ring-primary/20 transition-all"
            value={text}
            onChange={(e) => setText(e.target.value)}
            />
            <button
            type="submit"
            disabled={!text.trim() || loading}
            className="p-3 bg-primary text-white dark:text-zinc-900 rounded-full active:scale-90 transition-transform disabled:opacity-50 shadow-md shadow-primary/20"
            >
            <Send size={18} strokeWidth={3} />
            </button>
        </form>
      </div>
    </div>
  );
};
