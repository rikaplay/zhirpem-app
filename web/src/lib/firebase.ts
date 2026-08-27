import { initializeApp, getApps, getApp } from "firebase/app";
import { getFirestore } from "firebase/firestore";
import { getAuth } from "firebase/auth";

const firebaseConfig = {
  apiKey: process.env.NEXT_PUBLIC_FIREBASE_API_KEY,
  authDomain: process.env.NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN,
  projectId: process.env.NEXT_PUBLIC_FIREBASE_PROJECT_ID,
  storageBucket: process.env.NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: process.env.NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID,
  appId: process.env.NEXT_PUBLIC_FIREBASE_APP_ID
};

// Проверка на валидность API ключа (не должен содержать пробелов или быть пустым)
const isValidConfig = firebaseConfig.apiKey && !firebaseConfig.apiKey.includes(' ');

let app;
if (typeof window !== "undefined") { // Инициализируем только в браузере
    if (getApps().length === 0 && isValidConfig) {
        app = initializeApp(firebaseConfig);
    } else {
        app = getApp();
    }
}

export const db = app ? getFirestore(app) : null;
export const auth = app ? getAuth(app) : null;
export default app;
