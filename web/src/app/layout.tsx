import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";

const inter = Inter({ subsets: ["latin"] });

export const metadata: Metadata = {
  title: "Жирпем Web",
  description: "Зеркало приложения Жирпем",
  viewport: "width=device-width, initial-scale=1, maximum-scale=1",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ru" className="light">
      <body className={`${inter.className} antialiased transition-colors duration-300`}>
        {children}
      </body>
    </html>
  );
}
