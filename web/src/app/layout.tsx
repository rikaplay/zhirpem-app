import type { Metadata, Viewport } from "next";
import { Inter } from "next/font/google";
import "./globals.css";

const inter = Inter({ subsets: ["latin"] });

export const metadata: Metadata = {
  title: "Жирпем Web",
  description: "Зеркало приложения Жирпем",
  appleWebApp: {
    capable: true,
    statusBarStyle: "default",
    title: "Жирпем",
  },
  icons: {
    icon: "/icon.png",
    apple: "/icon.png",
  }
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  maximumScale: 1,
  userScalable: false,
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ru" className="light">
      <head>
        <link rel="manifest" href="/manifest.json" />
        <meta name="apple-mobile-web-app-capable" content="yes" />
      </head>
      <body className={`${inter.className} antialiased bg-background-light dark:bg-background-dark transition-colors duration-300`}>
        {children}
      </body>
    </html>
  );
}
