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
    webpack: (config) => {
        config.module.rules.push({
            test: /\.m?js$/,
            type: "javascript/auto",
            resolve: {
                fullySpecified: false,
            },
        });
        return config;
    },
};

export default nextConfig;
