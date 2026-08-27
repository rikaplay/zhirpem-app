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
    updateDoc,
    arrayUnion,
    arrayRemove,
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

        // 1. Try to get interests
        const interestDoc = await getDoc(doc(db, "user_interests", username));
        let recommendedPosts: Post[] = [];
        let newLastVisible = lastVisible;

        if (interestDoc.exists()) {
            const data = interestDoc.data();
            const scores = data.scores || {};
            const topTags = Object.entries(scores)
                .sort(([, a]: any, [, b]: any) => b - a)
                .slice(0, 10)
                .map(([tag]) => tag);

            if (topTags.length > 0) {
                let q = query(
                    collection(db, "zhirpem_posts"),
                    where("tags", "array-contains-any", topTags),
                    limit(PAGE_SIZE)
                );

                if (lastVisible) {
                    q = query(q, startAfter(lastVisible));
                }

                const snapshot = await getDocs(q);
                recommendedPosts = snapshot.docs.map(doc => ({
                    id: doc.id,
                    ...doc.data()
                } as Post));
                newLastVisible = snapshot.docs[snapshot.docs.length - 1] || lastVisible;
            }
        }

        // 2. Fallback to general posts if we don't have enough recommended ones
        if (recommendedPosts.length < 5) {
            const generalResult = await this.fetchPosts(newLastVisible);
            const combined = [...recommendedPosts, ...generalResult.posts];
            // Unique by ID
            const unique = combined.filter((v, i, a) => a.findIndex(t => t.id === v.id) === i);
            return {
                posts: unique,
                lastVisible: generalResult.lastVisible,
                isLastPage: generalResult.isLastPage
            };
        }

        return {
            posts: recommendedPosts,
            lastVisible: newLastVisible,
            isLastPage: false
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
