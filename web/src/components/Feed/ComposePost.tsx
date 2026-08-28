"use client";

import React, { useState, useRef } from 'react';
import { db } from '@/lib/firebase';
import { collection, addDoc, serverTimestamp } from 'firebase/firestore';
import { X, Image as ImageIcon, Video, Send, Loader2 } from 'lucide-react';
import { hapticFeedback } from '@/lib/utils';

export const ComposePost: React.FC<any> = ({ user, onClose, onSuccess }) => {
  const [text, setText] = useState('');
  const [loading, setLoading] = useState(false);
  const [mediaFile, setMediaFile] = useState<File | null>(null);
  const [mediaPreview, setMediaPreview] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setMediaFile(file);
      setMediaPreview(URL.createObjectURL(file));
    }
  };

  const uploadToCloudinary = async (file: File) => {
    const formData = new FormData();
    formData.append("file", file);
    formData.append("upload_preset", "mediapres");
    const res = await fetch(`https://api.cloudinary.com/v1_1/${process.env.NEXT_PUBLIC_CLOUDINARY_CLOUD_NAME}/image/upload`, {
      method: "POST",
      body: formData
    });
    const data = await res.json();
    return data.secure_url;
  };

  const handlePublish = async () => {
    if (!text.trim() && !mediaFile) return;
    setLoading(true);
    try {
      let mediaUrl = "";
      let mediaType = "NONE";

      if (mediaFile) {
        mediaUrl = await uploadToCloudinary(mediaFile);
        mediaType = mediaFile.type.startsWith('video') ? "VIDEO" : "IMAGE";
      }

      const date = new Date();
      await addDoc(collection(db, "zhirpem_posts"), {
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
        isMedia: !!mediaUrl,
        mediaUrl,
        mediaType,
        authorAvatarUrl: user.avatarUrl || null,
        authorNameColor: user.nameColor || null,
        blueBadge: user.blueBadge || false,
        yellowBadge: user.yellowBadge || false,
        authorStatus: user.status || "",
        timestamp: serverTimestamp(),
        tags: text.match(/#[a-zA-Zа-яА-Я0-9_]+/g) || []
      });

      hapticFeedback(50);
      onSuccess();
      onClose();
    } catch (e) {
        console.error(e);
        alert("Ошибка публикации");
    } finally { setLoading(false); }
  };

  return (
    <div className="fixed inset-0 z-[60] flex items-end sm:items-center justify-center p-0 sm:p-4">
      <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={onClose} />
      <div className="relative w-full max-w-[500px] bg-white dark:bg-zinc-900 rounded-t-[32px] sm:rounded-[32px] shadow-2xl flex flex-col p-6 animate-in slide-in-from-bottom duration-300">
        <div className="flex items-center justify-between mb-6">
          <button onClick={onClose} className="p-2 hover:bg-zinc-100 rounded-full"><X size={24} /></button>
          <button onClick={handlePublish} disabled={loading} className="bg-primary text-white px-6 py-2 rounded-full font-black uppercase text-sm tracking-widest disabled:opacity-50 flex items-center gap-2">
            {loading && <Loader2 size={16} className="animate-spin" />} {loading ? 'Грузим...' : 'Опубликовать'}
          </button>
        </div>

        <textarea autoFocus className="w-full h-40 bg-transparent text-xl font-medium outline-none resize-none placeholder:text-zinc-300" placeholder={`Что нового, ${user.name}?`} value={text} onChange={e => setText(e.target.value)} />

        {mediaPreview && (
            <div className="relative w-24 h-24 mb-4 rounded-2xl overflow-hidden shadow-md">
                <img src={mediaPreview} className="w-full h-full object-cover" />
                <button onClick={() => {setMediaFile(null); setMediaPreview(null);}} className="absolute top-1 right-1 bg-black/50 p-1 rounded-full text-white"><X size={12}/></button>
            </div>
        )}

        <div className="flex gap-4 border-t pt-4">
          <input type="file" hidden ref={fileInputRef} onChange={handleFileChange} accept="image/*,video/*" />
          <button onClick={() => fileInputRef.current?.click()} className="p-4 bg-zinc-100 dark:bg-zinc-800 rounded-2xl text-primary"><ImageIcon size={22} /></button>
          <button onClick={() => fileInputRef.current?.click()} className="p-4 bg-zinc-100 dark:bg-zinc-800 rounded-2xl text-primary"><Video size={22} /></button>
        </div>
      </div>
    </div>
  );
};
