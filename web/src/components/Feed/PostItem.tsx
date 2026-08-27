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
  Star
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
  // Защита от undefined: если поля в базе нет, используем пустой массив
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

    try {
      await FeedRepository.toggleLike(post.id, myUsername, isLiked);
    } catch (error) {
      console.error("Like error:", error);
      setIsLiked(isLiked);
      setLocalLikes(localLikes);
    }
  };

  const handleBookmark = async (e: React.MouseEvent) => {
    e.stopPropagation();
    if (!myUsername) return;
    setIsBookmarked(!isBookmarked);
    await FeedRepository.toggleBookmark(post.id, myUsername, isBookmarked);
  };

  const handleRepost = async (e: React.MouseEvent) => {
    e.stopPropagation();
    if (!myUsername) return;
    setIsReposted(!isReposted);
    await FeedRepository.toggleRepost(post.id, myUsername, isReposted);
  };

  const renderMedia = () => {
    const url = post.mediaUrl || post.imageUrl;
    if (!url) return null;

    if (post.mediaType === MediaType.VIDEO) {
        return (
          <video
            src={url}
            controls
            className="w-full rounded-2xl mt-2 max-h-[500px] bg-black"
          />
        );
    }

    return (
      <img
        src={url}
        alt="Post content"
        className="w-full rounded-2xl object-cover max-h-[500px] mt-2 cursor-pointer"
        loading="lazy"
      />
    );
  };

  return (
    <div
      className="bg-white dark:bg-zinc-900 rounded-[32px] p-5 shadow-sm border border-zinc-100 dark:border-zinc-800 transition-all active:scale-[0.98] cursor-pointer mb-4"
      onClick={() => setIsExpanded(!isExpanded)}
    >
      <div className="flex gap-3 items-start mb-3">
        <div
          className="w-12 h-12 rounded-full overflow-hidden bg-zinc-200 dark:bg-zinc-800 flex-shrink-0 cursor-pointer"
          onClick={(e) => { e.stopPropagation(); onUserClick(post.handle?.replace('@', '') || ''); }}
        >
          {post.authorAvatarUrl ? (
            <img src={post.authorAvatarUrl} alt={post.author} className="w-full h-full object-cover" />
          ) : (
            <div className="w-full h-full flex items-center justify-center text-primary font-bold text-xl">
              {post.author?.charAt(0).toUpperCase() || '?'}
            </div>
          )}
        </div>

        <div className="flex-1 min-w-0">
          <div className="flex items-center justify-between">
            <div className="flex flex-col" onClick={(e) => { e.stopPropagation(); onUserClick(post.handle?.replace('@', '') || ''); }}>
              <div className="flex items-center gap-1">
                <span className="font-bold text-[16px] truncate" style={{ color: post.authorNameColor || 'inherit' }}>
                  {post.author}
                </span>
                {post.blueBadge && <Verified size={16} className="text-blue-500 fill-blue-500" />}
                {post.yellowBadge && <Star size={16} className="text-yellow-500 fill-yellow-500" />}
                <span className="text-zinc-500 text-sm truncate ml-1">{post.handle}</span>
              </div>
            </div>
            <button className="p-2 text-zinc-400 hover:bg-zinc-100 dark:hover:bg-zinc-800 rounded-full transition-colors">
              <MoreVertical size={20} />
            </button>
          </div>
          <div className="text-zinc-400 text-[12px] mt-0.5">
            {post.date} {post.time && `в ${post.time}`}
          </div>
        </div>
      </div>

      <div className="mt-2">
        <p className={`text-[15px] leading-relaxed whitespace-pre-wrap ${!isExpanded ? 'line-clamp-3' : ''}`}>
          {post.text?.split(/(#[a-zA-Zа-яА-Я0-9_]+)/g).map((part, i) => {
            if (part.startsWith('#')) {
              return (
                <span key={i} className="text-primary font-bold cursor-pointer hover:underline" onClick={(e) => { e.stopPropagation(); onHashtagClick(part); }}>
                  {part}
                </span>
              );
            }
            return part;
          })}
        </p>
        {renderMedia()}
      </div>

      <div className="flex items-center gap-6 mt-4">
        <button className={`flex items-center gap-1.5 py-1 px-2 rounded-xl transition-all active:scale-90 ${post.commentsCount > 0 ? 'text-primary' : 'text-zinc-500'}`}>
          <span className="text-sm font-medium">Ответить {post.commentsCount > 0 && `(${post.commentsCount})`}</span>
        </button>

        <button className={`flex items-center gap-1.5 py-1 px-2 rounded-xl transition-all active:scale-90 ${isLiked ? 'text-pink-500' : 'text-zinc-500'}`} onClick={handleLike}>
          <Heart size={18} className={isLiked ? 'fill-pink-500' : ''} />
          <span className="text-sm font-bold">{localLikes}</span>
        </button>
      </div>

      <div className="h-[1px] bg-zinc-100 dark:bg-zinc-800 my-3 w-full" />

      <div className="flex items-center justify-between">
        <button className={`flex items-center gap-1.5 py-1 px-2 rounded-xl transition-all active:scale-90 ${isBookmarked ? 'text-primary' : 'text-zinc-400'}`} onClick={handleBookmark}>
          <Bookmark size={18} className={isBookmarked ? 'fill-primary' : ''} />
          <span className="text-[13px]">В закладки</span>
        </button>
        <div className="flex items-center gap-3">
          <button className={`flex items-center gap-1.5 py-1 px-2 rounded-xl transition-all active:scale-90 ${isReposted ? 'text-green-500' : 'text-zinc-400'}`} onClick={handleRepost}>
            <Repeat size={18} />
            <span className="text-[13px]">Репост</span>
          </button>
          <button className="p-2 text-zinc-400 hover:bg-zinc-100 dark:hover:bg-zinc-800 rounded-full transition-colors">
            <Share2 size={20} />
          </button>
        </div>
      </div>
    </div>
  );
};
