/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}"
  ],
  theme: {
    extend: {
      colors: {
        'brand-bg': '#FAF7F2',
        'brand-card': '#FFFFFF',
        'sa-blue': '#002395',
        'sa-green': '#007A4D',
        'sa-gold': '#FFB612',
        'sa-red': '#DE3831',
        'brand-text': '#1A1A1A',
      },
    },
  },
  plugins: [require("daisyui")],
}

