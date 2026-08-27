/** @type {import('next').NextConfig} */
const nextConfig = {
    output: 'export', // Включаем статический экспорт для стабильности на Netlify
    images: {
        unoptimized: true, // Нужно для режима export
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
