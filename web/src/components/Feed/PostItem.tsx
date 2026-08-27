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
  MessageCircle
} from 'lucide-react';
import { FeedRepository } from '@/repositories/FeedRepository';

interface PostItemProps {
  post: Post;
  myUsername: string;
  onUserClick: (username: string) => void;
  onHashtagClick: (hashtag: string) => void;
}

export const PostItem: React.FC<PostItemProps> = ({
  post,
  myUsername,
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

  const renderMedia = () => {
    const url = post.mediaUrl || post.imageUrl;
    if (!url) return null;

    if (post.mediaType === MediaType.VIDEO) {
      return (
        <video src={url} controls className="w-full rounded-2xl mt-3 max-h-[500px] bg-black shadow-inner" />
      );
    }

    return (
      <img
        src={url}
        alt="Post content"
        className="w-full rounded-2xl object-cover max-h-[500px] mt-3 cursor-pointer shadow-sm hover:opacity-95 transition-opacity"
        loading="lazy"
      />
    );
  };

  return (
    <div
      className="bg-white dark:bg-zinc-900 rounded-[32px] p-5 shadow-md border border-zinc-100 dark:border-zinc-800 transition-all active:scale-[0.99] cursor-pointer mb-4"
      onClick={() => setIsExpanded(!isExpanded)}
    >
      {/* HEADER: Avatar + Name + Badges */}
      <div className="flex gap-3 items-center mb-3">
        <div
          className="w-[46px] h-[46px] rounded-full overflow-hidden bg-primary/10 flex-shrink-0 cursor-pointer border-2 border-white dark:border-zinc-800 shadow-sm"
          onClick={(e) => { e.stopPropagation(); onUserClick(post.handle?.replace('@', '') || ''); }}
        >
          {post.authorAvatarUrl ? (
            <img src={post.authorAvatarUrl} alt={post.author} className="w-full h-full object-cover" />
          ) : (
            <div className="w-full h-full flex items-center justify-center text-primary font-black text-lg">
              {post.author?.charAt(0).toUpperCase() || '?'}
            </div>
          )}
        </div>

        <div className="flex-1 min-w-0">
          <div className="flex items-center justify-between">
            <div className="flex flex-col overflow-hidden" onClick={(e) => { e.stopPropagation(); onUserClick(post.handle?.replace('@', '') || ''); }}>
              <div className="flex items-center gap-1.5 flex-wrap">
                <span className="font-bold text-[15px] truncate text-zinc-900 dark:text-zinc-100" style={{ color: post.authorNameColor || '' }}>
                  {post.author}
                </span>
                <div className="flex items-center gap-0.5">
                  {post.blueBadge && <Verified size={15} className="text-blue-500 fill-blue-500" />}
                  {post.yellowBadge && <Star size={15} className="text-yellow-500 fill-yellow-500" />}
                </div>
                <span className="text-zinc-400 text-xs truncate font-medium">{post.handle}</span>
              </div>
              {post.authorStatus && (
                <span className="text-primary text-[11px] font-bold leading-tight mt-0.5 truncate uppercase tracking-wider">
                  {post.authorStatus}
                </span>
              )}
            </div>
            <button className="p-1.5 text-zinc-300 hover:text-zinc-500 rounded-full transition-colors">
              <MoreVertical size={18} />
            </button>
          </div>
          <div className="text-zinc-400 text-[10px] font-bold uppercase tracking-tight mt-0.5">
            {post.date} {post.time && `в ${post.time}`}
          </div>
        </div>
      </div>

      {/* CONTENT: Text */}
      <div className="mt-1 px-1">
        <p className={`text-[15px] leading-relaxed dark:text-zinc-200 whitespace-pre-wrap ${!isExpanded ? 'line-clamp-4' : ''}`}>
          {post.text?.split(/(#[a-zA-Zа-яА-Я0-9_]+)/g).map((part, i) => {
            if (part.startsWith('#')) {
              return (
                <span key={i} className="text-primary font-black cursor-pointer hover:underline" onClick={(e) => { e.stopPropagation(); onHashtagClick(part); }}>
                  {part}
                </span>
              );
            }
            return part;
          })}
        </p>

        {!isExpanded && (post.text?.length || 0) > 180 && (
          <button className="text-primary font-black text-xs mt-2 uppercase tracking-wider">Читать далее...</button>
        )}

        {/* MEDIA: Image or Video */}
        {renderMedia()}
      </div>

      {/* FOOTER: Actions */}
      <div className="flex items-center gap-5 mt-5 px-1">
        <button className={`flex items-center gap-1.5 py-1.5 px-3 rounded-full glass transition-all active:scale-90 ${post.commentsCount > 0 ? 'text-primary' : 'text-zinc-500'}`}>
          <MessageCircle size={16} />
          <span className="text-xs font-black uppercase tracking-wider">{post.commentsCount > 0 ? post.commentsCount : 'Ответ'}</span>
        </button>

        <button
          className={`flex items-center gap-1.5 py-1.5 px-3 rounded-full glass transition-all active:scale-90 ${isLiked ? 'text-pink-500 border-pink-500/20' : 'text-zinc-500'}`}
          onClick={handleLike}
        >
          <Heart size={16} className={isLiked ? 'fill-pink-500' : ''} />
          <span className="text-xs font-black uppercase tracking-wider">{localLikes}</span>
        </button>
      </div>

      <div className="h-[1px] bg-zinc-50 dark:bg-zinc-800/50 mt-4 mb-3 w-full" />

      <div className="flex items-center justify-between px-1">
        <button className={`flex items-center gap-1.5 p-1 transition-all active:scale-90 ${isBookmarked ? 'text-primary' : 'text-zinc-400'}`} onClick={(e) => { e.stopPropagation(); setIsBookmarked(!isBookmarked); }}>
          <Bookmark size={18} className={isBookmarked ? 'fill-primary' : ''} />
          <span className="text-[11px] font-bold uppercase tracking-widest">Закладки</span>
        </button>
        <div className="flex items-center gap-4">
          <button className={`flex items-center gap-1.5 p-1 transition-all active:scale-90 ${isReposted ? 'text-green-500' : 'text-zinc-400'}`} onClick={(e) => { e.stopPropagation(); setIsReposted(!isReposted); }}>
            <Repeat size={18} />
            <span className="text-[11px] font-bold uppercase tracking-widest">Репост</span>
          </button>
          <button className="p-1.5 text-zinc-400 hover:text-zinc-600 transition-colors">
            <Share2 size={18} />
          </button>
        </div>
      </div>
    </div>
  );
};
