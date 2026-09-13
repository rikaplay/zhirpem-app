/** @type {import('next').NextConfig} */
const nextConfig = {
    /* Мы убираем 'export', чтобы Vercel использовал свои нативные функции Next.js */
    images: {
        remotePatterns: [
            {
                protocol: 'https',
                hostname: 'res.cloudinary.com',
                pathname: '/**',
            },
        ],
    },
};

export default nextConfig;
