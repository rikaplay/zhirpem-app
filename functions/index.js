const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");
const axios = require("axios");

admin.initializeApp();

// Используйте firebase functions:secrets:set для реальной безопасности
const ONESIGNAL_APP_ID = "e52144a6-d4ea-46a4-870f-4089ec7a6af9";
const ONESIGNAL_REST_API_KEY = "os_v2_app_4uqujjwu5jdkjbypice6y6tk7hgycpqrjz7el4eil7itrf3xlinih6ikgpfq6o5l43izejzh4wdmjtrszqsdjvzj455p7mvulqousny";

exports.onNewPostCreatedOneSignal = onDocumentCreated("zhirpem_posts/{postId}", async (event) => {
    const postData = event.data.data();
    if (!postData) return null;

    const authorName = postData.author || "Пользователь";
    const postText = postData.text || "выложил новый пост";

    console.log(`Начинаю отправку пуша OneSignal для поста: ${event.params.postId}`);

    const notificationBody = {
        app_id: ONESIGNAL_APP_ID,
        included_segments: ["Subscribed Users"], // "All" тоже работает, но "Subscribed Users" надежнее
        headings: { "ru": `Новый пост от ${authorName}` },
        contents: { "ru": postText },
        data: {
            postId: event.params.postId,
            type: "NEW_POST"
        },
        // Важно для Android 13+
        android_channel_id: "zhirpem_notifications"
    };

    try {
        const response = await axios.post("https://onesignal.com/api/v1/notifications", notificationBody, {
            headers: {
                "Content-Type": "application/json; charset=utf-8",
                "Authorization": `Basic ${ONESIGNAL_REST_API_KEY}`
            }
        });
        console.log("OneSignal API Response:", response.data);
    } catch (error) {
        if (error.response) {
            console.error("OneSignal API Error Data:", error.response.data);
            console.error("OneSignal API Error Status:", error.response.status);
        } else {
            console.error("Error message:", error.message);
        }
    }
    return null;
});
