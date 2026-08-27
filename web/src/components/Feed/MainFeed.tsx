"use client";

import React, { useEffect, useState } from 'react';
import { Post } from '@/types/chat';
import { FeedRepository } from '@/repositories/FeedRepository';
import { PostItem } from './PostItem';
import { DocumentSnapshot } from 'firebase/firestore';

interface MainFeedProps {
  myUsername: string;
}

export const MainFeed: React.FC<MainFeedProps> = ({ myUsername }) => {
  const [posts, setPosts] = useState<Post[]>([]);
  const [lastVisible, setLastVisible] = useState<DocumentSnapshot | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isLastPage, setIsLastPage] = useState(false);
  const [activeTab, setActiveTab] = useState('Для вас');

  const tabs = ['Для вас', 'Вы читаете', 'Популярное', 'Медиа'];

  const loadPosts = async (isRefresh = false) => {
    if (isLoading || (isLastPage && !isRefresh)) return;

    setIsLoading(true);
    try {
      const result = activeTab === 'Для вас'
        ? await FeedRepository.fetchForYouPosts(myUsername, isRefresh ? null : lastVisible)
        : await FeedRepository.fetchPosts(isRefresh ? null : lastVisible);

      if (isRefresh) {
        setPosts(result.posts);
      } else {
        setPosts(prev => {
            const combined = [...prev, ...result.posts];
            return combined.filter((v, i, a) => a.findIndex(t => t.id === v.id) === i);
        });
      }

      setLastVisible(result.lastVisible);
      setIsLastPage(result.isLastPage);
    } catch (error) {
      console.error("Load posts error:", error);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadPosts(true);
  }, [activeTab, myUsername]);

  useEffect(() => {
    const handleScroll = () => {
      if (window.innerHeight + window.scrollY >= document.body.offsetHeight - 800 && !isLoading && !isLastPage) {
        loadPosts();
      }
    };
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, [isLoading, isLastPage, lastVisible]);

  return (
    <div className="max-w-[500px] mx-auto w-full min-h-screen pb-32 bg-background-light dark:bg-background-dark">
      {/* Header / Logo */}
      <div className="sticky top-0 z-20 bg-background-light/80 dark:bg-background-dark/80 backdrop-blur-md px-6 py-4 flex items-center justify-between">
        <div className="text-2xl font-black text-primary">Жирпем</div>
        <div className="w-10 h-10 rounded-xl glass flex items-center justify-center text-xl cursor-pointer">👤</div>
      </div>

      {/* Tabs */}
      <div className="sticky top-[64px] z-10 px-4 py-2">
        <div className="glass p-1.5 rounded-[24px] flex gap-1 shadow-lg border-white/10">
            {tabs.map((tab) => (
            <button
                key={tab}
                onClick={() => setActiveTab(tab)}
                className={`flex-1 py-2.5 rounded-[18px] text-[13px] font-bold transition-all btn-bounce ${
                activeTab === tab
                    ? 'bg-primary text-white dark:text-zinc-900 shadow-md scale-[1.02]'
                    : 'text-zinc-500 hover:bg-white/10'
                }`}
            >
                {tab}
            </button>
            ))}
        </div>
      </div>

      {/* Feed List */}
      <div className="px-4 mt-4 space-y-4">
        {posts.map((post) => (
          <PostItem
            key={post.id}
            post={post}
            myUsername={myUsername}
            onUserClick={(uid) => console.log('Click user:', uid)}
            onHashtagClick={(tag) => console.log('Click hashtag:', tag)}
          />
        ))}

        {isLoading && (
          <div className="flex justify-center p-8">
            <div className="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin"></div>
          </div>
        )}

        {!isLoading && posts.length === 0 && (
          <div className="flex flex-col items-center justify-center py-24 text-zinc-500">
            <span className="text-5xl mb-4">📭</span>
            <p className="font-bold">Здесь пока пусто</p>
          </div>
        )}
      </div>
    </div>
  );
};
