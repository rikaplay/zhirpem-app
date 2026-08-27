import { db } from "../lib/firebase";
import {
    collection,
    query,
    orderBy,
    limit,
    startAfter,
    getDocs,
    doc,
    getDoc,
    where,
    runTransaction,
    FieldValue,
    updateDoc,
    arrayUnion,
    arrayRemove,
    increment,
    DocumentSnapshot
} from "firebase/firestore";
import { Post } from "../types/chat";

const PAGE_SIZE = 25;

export const FeedRepository = {
    async fetchPosts(lastVisible: DocumentSnapshot | null = null) {
        let q = query(
            collection(db, "zhirpem_posts"),
            orderBy("timestamp", "desc"),
            limit(PAGE_SIZE)
        );

        if (lastVisible) {
            q = query(q, startAfter(lastVisible));
        }

        const snapshot = await getDocs(q);
        const posts = snapshot.docs.map(doc => ({
            id: doc.id,
            ...doc.data()
        } as Post));

        return {
            posts,
            lastVisible: snapshot.docs[snapshot.docs.length - 1] || null,
            isLastPage: snapshot.size < PAGE_SIZE
        };
    },

    async fetchForYouPosts(username: string, lastVisible: DocumentSnapshot | null = null) {
        if (!username) return this.fetchPosts(lastVisible);

        const interestDoc = await getDoc(doc(db, "user_interests", username));
        if (!interestDoc.exists()) return this.fetchPosts(lastVisible);

        const data = interestDoc.data();
        const scores = data.scores || {};
        const topTags = Object.entries(scores)
            .sort(([, a]: any, [, b]: any) => b - a)
            .take(10)
            .map(([tag]) => tag);

        if (topTags.length === 0) return this.fetchPosts(lastVisible);

        let q = query(
            collection(db, "zhirpem_posts"),
            where("tags", "array-contains-any", topTags),
            limit(PAGE_SIZE)
        );

        if (lastVisible) {
            q = query(q, startAfter(lastVisible));
        }

        const snapshot = await getDocs(q);
        const posts = snapshot.docs.map(doc => ({
            id: doc.id,
            ...doc.data()
        } as Post));

        // Если по тегам мало постов, можно добавить fallback, но для простоты пока так
        return {
            posts,
            lastVisible: snapshot.docs[snapshot.docs.length - 1] || null,
            isLastPage: snapshot.size < PAGE_SIZE
        };
    },

    async toggleLike(postId: string, userId: string, isLiked: boolean) {
        const postRef = doc(db, "zhirpem_posts", postId);

        await runTransaction(db, async (transaction) => {
            const snapshot = await transaction.get(postRef);
            if (!snapshot.exists()) return;

            const currentLikes = snapshot.data().likes || 0;

            if (isLiked) {
                transaction.update(postRef, {
                    likedBy: arrayRemove(userId),
                    likes: Math.max(0, currentLikes - 1)
                });
            } else {
                transaction.update(postRef, {
                    likedBy: arrayUnion(userId),
                    likes: currentLikes + 1
                });
            }
        });
    },

    async toggleBookmark(postId: string, userId: string, isBookmarked: boolean) {
        const postRef = doc(db, "zhirpem_posts", postId);
        if (isBookmarked) {
            await updateDoc(postRef, { bookmarkedBy: arrayRemove(userId) });
        } else {
            await updateDoc(postRef, { bookmarkedBy: arrayUnion(userId) });
        }
    },

    async toggleRepost(postId: string, userId: string, isReposted: boolean) {
        const postRef = doc(db, "zhirpem_posts", postId);
        if (isReposted) {
            await updateDoc(postRef, { repostedBy: arrayRemove(userId) });
        } else {
            await updateDoc(postRef, { repostedBy: arrayUnion(userId) });
        }
    }
};

// Вспомогательный метод для TS, так как Array.prototype.take не стандартный
declare global {
    interface Array<T> {
        take(n: number): Array<T>;
    }
}
if (!Array.prototype.take) {
    Array.prototype.take = function(n) {
        return this.slice(0, n);
    };
}
