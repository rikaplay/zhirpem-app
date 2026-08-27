import { db } from "../lib/firebase";
import {
    collection,
    doc,
    addDoc,
    setDoc,
    updateDoc,
    runTransaction,
    getDoc,
    Timestamp
} from "firebase/firestore";
import { MediaType, Message, Notification } from "../types/chat";

const CLOUDINARY_UPLOAD_PRESET = "mediapres"; // Тот же пресет, что в Android
const CLOUDINARY_CLOUD_NAME = process.env.NEXT_PUBLIC_CLOUDINARY_CLOUD_NAME;

export const ChatRepository = {

    async uploadMediaToCloudinary(
        file: File,
        messageType: "voice" | "video_square" | "image",
        chatId: string,
        currentUserId: string,
        senderName: string = "",
        senderAvatar: string = ""
    ) {
        const resourceType = messageType === "image" ? "image" : "video";
        const formData = new FormData();
        formData.append("file", file);
        formData.append("upload_preset", CLOUDINARY_UPLOAD_PRESET);

        try {
            const response = await fetch(
                `https://api.cloudinary.com/v1_1/${CLOUDINARY_CLOUD_NAME}/${resourceType}/upload`,
                {
                    method: "POST",
                    body: formData,
                }
            );

            const data = await response.json();
            const secureUrl = data.secure_url;

            if (secureUrl) {
                await this.sendMessageToFirestore(
                    chatId,
                    currentUserId,
                    secureUrl,
                    messageType,
                    senderName,
                    senderAvatar
                );
            }
        } catch (error) {
            console.error("Cloudinary upload error:", error);
        }
    },

    async sendMessageToFirestore(
        chatId: string,
        senderId: string,
        mediaUrl: string,
        type: string,
        senderName: string,
        senderAvatar: string
    ) {
        const chatRef = doc(db, "chats", chatId);
        const messageRef = doc(collection(chatRef, "messages"));

        const mediaType = type === "image" ? MediaType.IMAGE : MediaType.VIDEO;

        const messageData: Message = {
            senderId,
            text: "",
            mediaUrl,
            mediaType,
            timestamp: Date.now(),
            replyToId: type === "voice" ? "voice" : null
        };

        await runTransaction(db, async (transaction) => {
            transaction.set(messageRef, messageData);
            transaction.update(chatRef, {
                lastMessage: "📎 Медиасообщение",
                lastMessageTimestamp: Date.now()
            });
        });

        // Уведомление собеседнику
        const peerId = chatId.split("_").find(id => id !== senderId) || "";
        if (peerId) {
            await this.checkRecipientAndNotify(
                senderId,
                senderName,
                senderAvatar,
                peerId,
                chatId,
                "📎 Медиасообщение"
            );
        }
    },

    async checkRecipientAndNotify(
        senderId: string,
        senderName: string,
        senderAvatar: string,
        receiverId: string,
        chatId: string,
        text: string
    ) {
        const userDocRef = doc(db, "users", receiverId);
        const userDoc = await getDoc(userDocRef);

        if (!userDoc.exists()) return;

        const userData = userDoc.data();
        const isOnlyVerified = userData.isOnlyVerifiedMessages ?? false;

        const proceedWithNotification = async () => {
            const status = userData.status ?? "offline";
            const currentScreen = userData.currentScreen ?? "";
            const expectedScreen = `ChatScreen_${senderId}`;

            // 1. Push notification (здесь должна быть логика FCM Web)
            console.log("Sending Push to", receiverId);

            // 2. Запись в ленту уведомлений
            if (status === "offline" || currentScreen !== expectedScreen) {
                const notificationData: Notification = {
                    senderId,
                    senderName,
                    senderAvatarUrl: senderAvatar,
                    receiverId,
                    type: "CHAT_MESSAGE",
                    text,
                    timestamp: Timestamp.now(),
                    chatId
                };
                await addDoc(collection(db, "notifications"), notificationData);
            }
        };

        if (isOnlyVerified) {
            const senderDocRef = doc(db, "users", senderId);
            const senderDoc = await getDoc(senderDocRef);
            if (senderDoc.exists()) {
                const senderData = senderDoc.data();
                const isVerified = senderData.blueBadge === true || senderData.yellowBadge === true;
                if (isVerified) {
                    await proceedWithNotification();
                }
            }
        } else {
            await proceedWithNotification();
        }
    }
};
