import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./src/pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/components/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/app/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: "#006B44",
          dark: "#8DE3B5",
        },
        background: {
          light: "#F7FBF8",
          dark: "#121413",
        },
        surface: {
          light: "#EEF2EE",
          dark: "#1A1D1C",
        }
      },
      borderRadius: {
        '3xl': '24px',
        '4xl': '32px',
      }
    },
  },
  plugins: [],
  darkMode: 'class',
};
export default config;
