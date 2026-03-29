import { describe, it, expect } from 'vitest';
import router from '../router';
import { mount } from '@vue/test-utils';
import { createPinia } from 'pinia';
import App from './App.vue';

describe('App', () => {
  it('renders properly', async () => {
    const wrapper = mount(App, {
      global: { plugins: [createPinia(), router] },
    });
    await router.isReady();
    expect(wrapper.text()).toContain('Simple Social Media');
  });
});
