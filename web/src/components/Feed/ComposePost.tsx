"use client";

import React, { useState } from 'react';
import { db } from '@/lib/firebase';
import { collection, addDoc, serverTimestamp } from 'firebase/firestore';
import { X, Image as ImageIcon, Video, Send } from 'lucide-react';

interface ComposePostProps {
  user: any;
  onClose: () => void;
  onSuccess: () => void;
}

export const ComposePost: React.FC<ComposePostProps> = ({ user, onClose, onSuccess }) => {
  const [text, setText] = useState('');
  const [loading, setLoading] = useState(false);

  const handlePublish = async () => {
    if (!text.trim()) return;
    setLoading(true);

    try {
      const date = new Date();
      const newPost = {
        author: user.name,
        handle: `@${user.id}`,
        text: text.trim(),
        date: date.toLocaleDateString('ru-RU', { day: 'numeric', month: 'short' }),
        time: date.toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' }),
        likes: 0,
        views: 0,
        likedBy: [],
        bookmarkedBy: [],
        repostedBy: [],
        commentsCount: 0,
        isMedia: false,
        mediaUrl: "",
        mediaType: "NONE",
        authorAvatarUrl: user.avatarUrl || null,
        timestamp: serverTimestamp(),
        tags: text.match(/#[a-zA-Zа-яА-Я0-9_]+/g) || []
      };

      await addDoc(collection(db, "zhirpem_posts"), newPost);
      onSuccess();
      onClose();
    } catch (error) {
      console.error("Error publishing post:", error);
      alert("Ошибка при публикации");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-[60] flex items-end sm:items-center justify-center p-0 sm:p-4">
      <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={onClose} />

      <div className="relative w-full max-w-[500px] bg-white dark:bg-zinc-900 rounded-t-[32px] sm:rounded-[32px] shadow-2xl flex flex-col p-6 animate-in slide-in-from-bottom duration-300">
        <div className="flex items-center justify-between mb-4">
          <button onClick={onClose} className="p-2 hover:bg-zinc-100 dark:hover:bg-zinc-800 rounded-full">
            <X size={24} />
          </button>
          <button
            onClick={handlePublish}
            disabled={loading || !text.trim()}
            className="bg-primary text-white dark:text-zinc-900 px-6 py-2 rounded-full font-black uppercase text-sm tracking-widest disabled:opacity-50 btn-bounce shadow-lg"
          >
            {loading ? '...' : 'Опубликовать'}
          </button>
        </div>

        <textarea
          autoFocus
          className="w-full h-40 bg-transparent text-xl font-medium outline-none resize-none placeholder:text-zinc-400"
          placeholder={`Что нового, ${user.name}?`}
          value={text}
          onChange={(e) => setText(e.target.value)}
        />

        <div className="flex items-center gap-4 mt-4 pt-4 border-t border-zinc-100 dark:border-zinc-800">
          <button className="p-3 bg-zinc-100 dark:bg-zinc-800 rounded-2xl text-primary btn-bounce">
            <ImageIcon size={22} />
          </button>
          <button className="p-3 bg-zinc-100 dark:bg-zinc-800 rounded-2xl text-primary btn-bounce">
            <Video size={22} />
          </button>
        </div>
      </div>
    </div>
  );
};
