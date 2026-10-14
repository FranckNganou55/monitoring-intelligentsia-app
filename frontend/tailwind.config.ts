import type { Config } from 'tailwindcss'
import forms from '@tailwindcss/forms'

const config: Config = {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        gray: {
          900: '#2C2C2C',
          700: '#4A4A4A',
          600: '#6B7280',
          200: '#E5E7EB',
          100: '#F3F4F6',
          50:  '#F5F5F5',
        },
        gold: {
          700: '#8B6914',
          600: '#A07D2A',
          500: '#C9A84C',
          400: '#E2C07A',
          300: '#F5DFA0',
          100: '#FDF8EC',
          50:  '#FFFDF5',
        },
        status: {
          ok:      '#22C55E',
          warning: '#F59E0B',
          error:   '#EF4444',
          unknown: '#6B7280',
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
        mono: ['JetBrains Mono', 'monospace'],
      },
      boxShadow: {
        card: '0 1px 3px 0 rgb(0 0 0 / 0.1), 0 1px 2px -1px rgb(0 0 0 / 0.1)',
        'card-hover': '0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1)',
      },
    },
  },
  plugins: [forms],
}

export default config
