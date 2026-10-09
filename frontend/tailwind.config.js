import { createRequire } from "node:module"

const require = createRequire(import.meta.url)

/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}"
  ],
  theme: {
    extend: {}
  },
  plugins: [require("daisyui")],
  daisyui: {
    themes: [
      {
        optigridlight: {
          primary: "#F2610C",
          secondary: "#1E7A6B",
          accent: "#A8730B",
          neutral: "#1A1714",
          "base-100": "#FBF9F6",
          "base-200": "#F1EDE6",
          "base-300": "#E4DDD2",
          info: "#4A5A63",
          success: "#1E7A6B",
          warning: "#A8730B",
          error: "#C0341B",
        },
      },{
        optigriddark: {
          primary: "#FF7A2E",
          secondary: "#46B49F",
          accent: "#E0A94B",
          neutral: "#F5F0E9",
          "base-100": "#1E1A16",
          "base-200": "#15120F",
          "base-300": "#251F1A",
          info: "#93A4AD",
          success: "#46B49F",
          warning: "#E0A94B",
          error: "#FF7A63",
        },
      },
    ],
  },
}
