"use client";

import React, { useState } from 'react';
import { Post, MediaType } from '@/types/chat';
import {
  Heart,
  Bookmark,
  Repeat,
  Share2,
  MoreVertical,
  Verified,
  Star,
  MessageCircle,
  Edit2,
  Trash2,
  AlertTriangle
} from 'lucide-react';
import { FeedRepository } from '@/repositories/FeedRepository';
import { db } from '@/lib/firebase';
import { doc, deleteDoc, updateDoc } from 'firebase/firestore';
import { Comments } from './Comments';

interface PostItemProps {
  post: Post;
  myUsername: string;
  myUser: any;
  onUserClick: (username: string) => void;
  onHashtagClick: (hashtag: string) => void;
}

export const PostItem: React.FC<PostItemProps> = ({
  post,
  myUsername,
  myUser,
  onUserClick,
  onHashtagClick
}) => {
  const likedBy = post.likedBy || [];
  const bookmarkedBy = post.bookmarkedBy || [];
  const repostedBy = post.repostedBy || [];

  const [localLikes, setLocalLikes] = useState(post.likes || 0);
  const [isLiked, setIsLiked] = useState(likedBy.includes(myUsername));
  const [isBookmarked, setIsBookmarked] = useState(bookmarkedBy.includes(myUsername));
  const [isReposted, setIsReposted] = useState(repostedBy.includes(myUsername));
  const [isExpanded, setIsExpanded] = useState(false);
  const [showComments, setShowComments] = useState(false);
  const [showMenu, setShowMenu] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [editText, setEditText] = useState(post.text);

  const isMyPost = post.handle?.replace('@', '') === myUsername;

  const handleLike = async (e: React.MouseEvent) => {
    e.stopPropagation();
    if (!myUsername) return;
    const newIsLiked = !isLiked;
    setIsLiked(newIsLiked);
    setLocalLikes(prev => newIsLiked ? prev + 1 : Math.max(0, prev - 1));
    try { await FeedRepository.toggleLike(post.id, myUsername, isLiked); } catch (error) {
      setIsLiked(isLiked); setLocalLikes(localLikes);
    }
  };

  const handleDelete = async () => {
    if (window.confirm("Удалить этот пост?")) {
        await deleteDoc(doc(db, "zhirpem_posts", post.id));
    }
  };

  const handleUpdate = async () => {
    await updateDoc(doc(db, "zhirpem_posts", post.id), { text: editText });
    setIsEditing(false);
  };

  const handleReport = async () => {
    await updateDoc(doc(db, "zhirpem_posts", post.id), { status: "на рассмотрении" });
    alert("Жалоба отправлена модераторам");
    setShowMenu(false);
  };

  const renderMedia = () => {
    const url = post.mediaUrl || post.imageUrl;
    if (!url) return null;
    if (post.mediaType === MediaType.VIDEO) {
      return <video src={url} controls className="w-full rounded-2xl mt-3 max-h-[500px] bg-black shadow-inner" />;
    }
    return <img src={url} alt="" className="w-full rounded-2xl object-cover max-h-[500px] mt-3 shadow-sm" loading="lazy" />;
  };

  return (
    <div className="bg-white dark:bg-zinc-900 rounded-[32px] p-5 shadow-md border border-zinc-100 dark:border-zinc-800 transition-all mb-4 relative" onClick={() => setIsExpanded(!isExpanded)}>

      {/* Context Menu */}
      {showMenu && (
        <div className="absolute right-6 top-14 z-20 bg-white dark:bg-zinc-800 rounded-2xl shadow-xl border border-zinc-500/10 overflow-hidden w-48 animate-in fade-in zoom-in duration-200" onClick={e => e.stopPropagation()}>
            {isMyPost ? (
                <>
                    <button onClick={() => {setIsEditing(true); setShowMenu(false);}} className="w-full flex items-center gap-3 p-4 hover:bg-zinc-50 dark:hover:bg-zinc-700/50 text-sm font-bold"><Edit2 size={16}/> Изменить</button>
                    <button onClick={handleDelete} className="w-full flex items-center gap-3 p-4 hover:bg-red-50 dark:hover:bg-red-900/20 text-red-500 text-sm font-bold"><Trash2 size={16}/> Удалить</button>
                </>
            ) : (
                <button onClick={handleReport} className="w-full flex items-center gap-3 p-4 hover:bg-red-50 dark:hover:bg-red-900/20 text-red-500 text-sm font-bold"><AlertTriangle size={16}/> Пожаловаться</button>
            )}
        </div>
      )}

      <div className="flex gap-3 items-center mb-3">
        <div className="w-[46px] h-[46px] rounded-full overflow-hidden bg-primary/10 flex-shrink-0 cursor-pointer border-2 border-white dark:border-zinc-800" onClick={(e) => { e.stopPropagation(); onUserClick(post.handle?.replace('@', '') || ''); }}>
          {post.authorAvatarUrl ? <img src={post.authorAvatarUrl} className="w-full h-full object-cover" /> : <div className="w-full h-full flex items-center justify-center text-primary font-black text-lg">{post.author?.charAt(0)}</div>}
        </div>

        <div className="flex-1 min-w-0">
          <div className="flex items-center justify-between">
            <div className="flex flex-col" onClick={(e) => { e.stopPropagation(); onUserClick(post.handle?.replace('@', '') || ''); }}>
              <div className="flex items-center gap-1.5 flex-wrap">
                <span className="font-bold text-[15px] truncate text-zinc-900 dark:text-zinc-100" style={{ color: post.authorNameColor || '' }}>{post.author}</span>
                {post.blueBadge && <Verified size={15} className="text-blue-500 fill-blue-500" />}
                <span className="text-zinc-400 text-xs font-medium">{post.handle}</span>
              </div>
            </div>
            <button onClick={(e) => {e.stopPropagation(); setShowMenu(!showMenu);}} className="p-2 text-zinc-300 hover:text-zinc-500 rounded-full"><MoreVertical size={20} /></button>
          </div>
          <div className="text-zinc-400 text-[10px] font-bold uppercase mt-0.5">{post.date} {post.time && `в ${post.time}`}</div>
        </div>
      </div>

      <div className="mt-1 px-1">
        {isEditing ? (
            <div className="space-y-3" onClick={e => e.stopPropagation()}>
                <textarea className="w-full bg-zinc-50 dark:bg-zinc-800 rounded-2xl p-4 text-sm font-medium outline-none border border-primary/20" value={editText} onChange={e => setEditText(e.target.value)} />
                <div className="flex gap-2">
                    <button onClick={() => setIsEditing(false)} className="flex-1 py-2 text-xs font-bold uppercase text-zinc-500">Отмена</button>
                    <button onClick={handleUpdate} className="flex-1 py-2 bg-primary text-white rounded-xl text-xs font-black uppercase">Сохранить</button>
                </div>
            </div>
        ) : (
            <p className={`text-[15px] leading-relaxed dark:text-zinc-200 whitespace-pre-wrap ${!isExpanded ? 'line-clamp-4' : ''}`}>
                {post.text?.split(/(#[a-zA-Zа-яА-Я0-9_]+)/g).map((part, i) => part.startsWith('#') ? <span key={i} className="text-primary font-black cursor-pointer hover:underline" onClick={(e) => { e.stopPropagation(); onHashtagClick(part); }}>{part}</span> : part)}
            </p>
        )}
        {!isEditing && renderMedia()}
      </div>

      <div className="flex items-center gap-5 mt-5 px-1">
        <button onClick={(e) => { e.stopPropagation(); setShowComments(!showComments); }} className={`flex items-center gap-1.5 py-1.5 px-3 rounded-full glass transition-all ${post.commentsCount > 0 ? 'text-primary' : 'text-zinc-500'}`}><MessageCircle size={16} /><span className="text-xs font-black uppercase">{post.commentsCount || 'Ответ'}</span></button>
        <button className={`flex items-center gap-1.5 py-1.5 px-3 rounded-full glass transition-all ${isLiked ? 'text-pink-500' : 'text-zinc-500'}`} onClick={handleLike}><Heart size={16} className={isLiked ? 'fill-pink-500' : ''} /><span className="text-xs font-black uppercase">{localLikes}</span></button>
      </div>

      {showComments && <Comments postId={post.id} myUser={myUser} />}
    </div>
  );
};
