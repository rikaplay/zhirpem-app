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
        setPosts(prev => [...prev, ...result.posts].filter((v, i, a) => a.findIndex(t => t.id === v.id) === i));
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

  // Infinite scroll logic
  useEffect(() => {
    const handleScroll = () => {
      if (window.innerHeight + window.scrollY >= document.body.offsetHeight - 500 && !isLoading && !isLastPage) {
        loadPosts();
      }
    };
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, [isLoading, isLastPage, lastVisible]);

  return (
    <div className="max-w-[600px] mx-auto w-full min-h-screen pb-24">
      {/* Tabs */}
      <div className="sticky top-0 z-10 glass p-2 rounded-[24px] mb-4 mx-3 mt-3 flex gap-1">
        {tabs.map((tab) => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            className={`flex-1 py-2.5 rounded-[20px] text-xs font-bold transition-all ${
              activeTab === tab
                ? 'bg-primary text-white dark:text-zinc-900'
                : 'text-zinc-500 hover:bg-zinc-100 dark:hover:bg-zinc-800'
            }`}
          >
            {tab}
          </button>
        ))}
      </div>

      {/* Feed */}
      <div className="px-3">
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
            <div className="w-6 h-6 border-2 border-primary border-t-transparent rounded-full animate-spin"></div>
          </div>
        )}

        {!isLoading && posts.length === 0 && (
          <div className="flex flex-col items-center justify-center py-20 text-zinc-500">
            <span className="text-4xl mb-4">📭</span>
            <p>Здесь пока пусто.</p>
          </div>
        )}
      </div>
    </div>
  );
};
