/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        ink: { DEFAULT: '#0E2F2A', soft: '#1D4A42' },
        chalk: { DEFAULT: '#F5F6F2', deep: '#E9ECE6' },
        tag: { DEFAULT: '#FFCF3F', deep: '#E9B71C' },
        moss: '#5C6F69',
        line: '#D3D9D3',
        leaf: '#1F7A5C',
        rust: { DEFAULT: '#B8382A', soft: '#FBEAE7' },
      },
      fontFamily: {
        display: ['"Bricolage Grotesque Variable"', 'ui-sans-serif', 'system-ui', 'sans-serif'],
        sans: ['"Figtree Variable"', 'ui-sans-serif', 'system-ui', 'sans-serif'],
      },
    },
  },
  plugins: [],
};
