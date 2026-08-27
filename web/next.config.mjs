/** @type {import('next').NextConfig} */
const nextConfig = {
    transpilePackages: ['firebase', '@firebase/auth', '@firebase/app', 'undici'],
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

