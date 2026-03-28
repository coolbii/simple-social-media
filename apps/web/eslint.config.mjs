import path from 'node:path';
import { fileURLToPath } from 'node:url';
import vue from 'eslint-plugin-vue';
import vueParser from 'vue-eslint-parser';
import tsEslint from 'typescript-eslint';
import vuePrettierConfig from '@vue/eslint-config-prettier';
import baseConfig from '../../eslint.config.mjs';

const tsconfigRootDir = path.dirname(fileURLToPath(import.meta.url));

export default [
  ...baseConfig,

  // Vue 3 recommended rules (template, directives, component structure)
  ...vue.configs['flat/recommended'],

  // TypeScript recommended rules (no-unused-vars, no-explicit-any, etc.)
  ...tsEslint.configs.recommended,

  // Must be last: disables ESLint rules that conflict with Prettier
  vuePrettierConfig,

  // Parser for <script lang="ts"> inside .vue files
  {
    files: ['**/*.vue'],
    languageOptions: {
      parser: vueParser,
      parserOptions: {
        parser: tsEslint.parser,
        tsconfigRootDir,
      },
    },
  },

  {
    files: ['**/*.{ts,tsx,js,jsx,vue,mjs,cjs}'],
    languageOptions: {
      parserOptions: {
        tsconfigRootDir,
      },
    },
  },

  {
    files: ['**/*.ts', '**/*.tsx', '**/*.js', '**/*.jsx', '**/*.vue'],
    rules: {
      // ── Vue 3: Composition API enforcement ───────────────────────
      // Allow single-word names (e.g. LoginView, HomeView)
      'vue/multi-word-component-names': 'off',
      // Enforce <script setup> or composition API only — no Options API
      'vue/component-api-style': ['error', ['script-setup', 'composition']],
      // Canonical macro order in <script setup>
      'vue/define-macros-order': ['error', { order: ['defineOptions', 'defineProps', 'defineEmits', 'defineSlots'] }],
      // Canonical block order: <script> → <template> → <style>
      'vue/block-order': ['error', { order: ['script', 'template', 'style'] }],
      // Catch unused template variables (e.g. v-for items)
      'vue/no-unused-vars': 'error',
      // Use vue exports from 'vue', not from '@vue/runtime-core' etc.
      'vue/prefer-import-from-vue': 'error',
      // v-html is an XSS risk — warn so it's a conscious choice
      'vue/no-v-html': 'warn',
      // Vue 3 allows multiple root elements in templates
      'vue/no-multiple-template-root': 'off',

      // ── TypeScript ────────────────────────────────────────────────
      // Warn rather than error on `any` — sometimes unavoidable with libs
      '@typescript-eslint/no-explicit-any': 'warn',
      // Catch unused variables; allow underscore-prefixed intentional skips
      '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_' }],
      // Disabled here because this config is not type-aware by default
      '@typescript-eslint/prefer-optional-chain': 'off',

      // ── General Best Practices ────────────────────────────────────
      // Allow console.warn / console.error for intentional logging
      'no-console': ['warn', { allow: ['warn', 'error'] }],
    },
  },
];
