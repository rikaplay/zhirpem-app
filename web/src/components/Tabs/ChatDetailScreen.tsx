"use client";

import React, { useState, useEffect, useRef } from 'react';
import { db } from '@/lib/firebase';
import {
    collection,
    query,
    orderBy,
    onSnapshot,
    addDoc,
    serverTimestamp,
    doc,
    updateDoc
} from 'firebase/firestore';
import { ArrowLeft, Send, Image as ImageIcon, Smile } from 'lucide-react';

interface ChatDetailScreenProps {
  chatId: string;
  myUsername: string;
  onBack: () => void;
}

export const ChatDetailScreen: React.FC<ChatDetailScreenProps> = ({ chatId, myUsername, onBack }) => {
  const [messages, setMessages] = useState<any[]>([]);
  const [text, setText] = useState('');
  const [loading, setLoading] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const peerUsername = chatId.split('_').find(p => p !== myUsername) || myUsername;

  useEffect(() => {
    const q = query(
      collection(db, "chats", chatId, "messages"),
      orderBy("timestamp", "asc")
    );

    const unsubscribe = onSnapshot(q, (snapshot) => {
      setMessages(snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() })));
    });

    return () => unsubscribe();
  }, [chatId]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  const sendMessage = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!text.trim() || loading) return;
    setLoading(true);

    try {
      const messageData = {
        senderId: myUsername,
        text: text.trim(),
        timestamp: Date.now(), // Используем миллисекунды для синхронизации с Android
        mediaType: "NONE",
        mediaUrl: ""
      };

      await addDoc(collection(db, "chats", chatId, "messages"), messageData);

      await updateDoc(doc(db, "chats", chatId), {
        lastMessage: text.trim(),
        lastMessageTimestamp: Date.now()
      });

      setText('');
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-[60] bg-background-light dark:bg-background-dark flex flex-col animate-in slide-in-from-right duration-300">
      {/* Header */}
      <div className="p-4 flex items-center gap-4 bg-white/80 dark:bg-zinc-900/80 backdrop-blur-md border-b border-zinc-500/5">
        <button onClick={onBack} className="p-1 hover:bg-zinc-100 dark:hover:bg-zinc-800 rounded-full"><ArrowLeft size={24} /></button>
        <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center font-bold text-primary">
            {peerUsername.charAt(0).toUpperCase()}
        </div>
        <div>
            <h2 className="font-black text-[16px]">@{peerUsername}</h2>
            <p className="text-[11px] text-green-500 font-bold uppercase tracking-widest">В сети</p>
        </div>
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto p-4 space-y-3 custom-scrollbar bg-[url('https://res.cloudinary.com/dcwp4nm3e/image/upload/v1/onboarding')] bg-fixed bg-center bg-cover bg-opacity-5">
        {messages.map((msg) => {
          const isMe = msg.senderId === myUsername;
          return (
            <div key={msg.id} className={`flex ${isMe ? 'justify-end' : 'justify-start'}`}>
              <div className={`max-w-[80%] px-4 py-2.5 rounded-[22px] shadow-sm ${
                isMe ? 'bg-primary text-white rounded-tr-none' : 'bg-white dark:bg-zinc-800 text-zinc-900 dark:text-zinc-100 rounded-tl-none'
              }`}>
                <p className="text-[15px] leading-relaxed">{msg.text}</p>
                <p className={`text-[10px] mt-1 text-right font-bold opacity-50`}>
                    {new Date(msg.timestamp).toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' })}
                </p>
              </div>
            </div>
          );
        })}
        <div ref={messagesEndRef} />
      </div>

      {/* Input */}
      <form onSubmit={sendMessage} className="p-4 bg-white dark:bg-zinc-900 border-t border-zinc-500/5 flex items-center gap-2">
        <button type="button" className="p-2 text-zinc-400 hover:text-primary transition-colors"><ImageIcon size={22} /></button>
        <input
            type="text"
            placeholder="Сообщение..."
            className="flex-1 bg-zinc-100 dark:bg-zinc-800 border-none rounded-2xl px-4 py-3 text-[15px] outline-none"
            value={text}
            onChange={(e) => setText(e.target.value)}
        />
        <button type="button" className="p-2 text-zinc-400 hover:text-primary transition-colors"><Smile size={22} /></button>
        <button
            type="submit"
            disabled={!text.trim() || loading}
            className="w-12 h-12 bg-primary text-white rounded-full flex items-center justify-center shadow-lg active:scale-90 transition-all disabled:opacity-50"
        >
            <Send size={20} strokeWidth={3} />
        </button>
      </form>
    </div>
  );
};
