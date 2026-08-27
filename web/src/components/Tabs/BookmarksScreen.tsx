"use client";

import React, { useState, useEffect } from 'react';
import { db } from '@/lib/firebase';
import { collection, query, where, orderBy, onSnapshot } from 'firebase/firestore';
import { PostItem } from '../Feed/PostItem';

export const BookmarksScreen = ({ myUser, onUserClick }: any) => {
  const [posts, setPosts] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const q = query(
      collection(db, "zhirpem_posts"),
      where("bookmarkedBy", "array-contains", myUser.id),
      orderBy("timestamp", "desc")
    );
    return onSnapshot(q, (snapshot) => {
      setPosts(snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() })));
      setLoading(false);
    });
  }, [myUser.id]);

  return (
    <div className="min-h-screen px-4 pt-4 pb-32 space-y-4">
      <h2 className="text-2xl font-black uppercase tracking-tighter text-zinc-400 mb-6">Закладки 🔖</h2>
      {loading ? (
        <div className="flex justify-center py-20"><div className="animate-spin rounded-full h-8 w-8 border-4 border-primary border-t-transparent" /></div>
      ) : posts.length > 0 ? (
        posts.map(post => <PostItem key={post.id} post={post} myUsername={myUser.id} myUser={myUser} onUserClick={onUserClick} onHashtagClick={() => {}} />)
      ) : (
        <div className="text-center py-40 opacity-20"><div className="text-6xl mb-4">🔖</div><p className="font-black uppercase">Нет закладок</p></div>
      )}
    </div>
  );
};
