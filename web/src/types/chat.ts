import { Timestamp } from "firebase/firestore";

export enum MediaType {
    IMAGE = "IMAGE",
    GIF = "GIF",
    VIDEO = "VIDEO",
    NONE = "NONE"
}

export interface PollData {
    question: string;
    options: string[];
    anonymous: boolean;
    multipleChoice: boolean;
    votes: { [key: string]: string[] };
}

export interface Post {
    id: string;
    author: string;
    date: string;
    handle: string;
    isMedia: boolean;
    imageUrl?: string | null;
    mediaUrl: string;
    mediaType: MediaType;
    authorAvatarUrl?: string | null;
    blueBadge: boolean;
    yellowBadge: boolean;
    likes: number;
    commentsCount: number;
    text: string;
    time: string;
    views: number;
    likedBy: string[];
    bookmarkedBy: string[];
    repostedBy: string[];
    timestamp: Timestamp | null;
    isAuthorBanned: boolean;
    authorNameColor?: string | null;
    communityId?: string | null;
    poll?: PollData | null;
    authorStatus?: string | null;
    tags: string[];
}

export interface Message {
    senderId: string;
    text: string;
    mediaUrl: string;
    mediaType: MediaType;
    timestamp: number;
    replyToId?: string | null;
}

export interface Chat {
    id: string;
    lastMessage: string;
    lastMessageTimestamp: number;
    participants: string[];
}

export interface User {
    uid: string;
    id: string; // В Firestore это часто document ID (username)
    name: string;
    username: string;
    status: string;
    currentScreen: string;
    isOnlyVerifiedMessages?: boolean;
    blueBadge?: boolean;
    yellowBadge?: boolean;
    avatarUrl?: string | null;
    bio?: string;
    bannerColor?: string;
    bannerUrl?: string | null;
    isBanned?: boolean;
}

export interface Notification {
    senderId: string;
    senderName: string;
    senderAvatarUrl: string;
    receiverId: string;
    type: 'CHAT_MESSAGE' | 'COMMENT' | 'LIKE';
    text: string;
    timestamp: Timestamp;
    chatId?: string;
    postId?: string;
    targetText?: string;
}
