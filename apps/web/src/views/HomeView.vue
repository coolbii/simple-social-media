<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { createPost, fetchPosts, uploadPostImage } from '../services/api';
import type { PostItem } from '../types';

const MAX_IMAGE_SIZE_BYTES = 1 * 1024 * 1024;

const posts = ref<PostItem[]>([]);
const isLoadingPosts = ref(true);
const loadingError = ref('');

const content = ref('');
const selectedImage = ref<File | null>(null);
const imagePreviewUrl = ref<string | null>(null);
const imageError = ref('');
const submitError = ref('');

const isSubmitting = ref(false);
const isUploadingImage = ref(false);
const fileInputRef = ref<HTMLInputElement | null>(null);

const canSubmit = computed(() => {
  return content.value.trim().length > 0 && !isSubmitting.value && imageError.value.length === 0;
});

function formatDateTime(dateTime: string): string {
  return new Date(dateTime).toLocaleString();
}

function revokePreviewUrl() {
  if (imagePreviewUrl.value) {
    URL.revokeObjectURL(imagePreviewUrl.value);
    imagePreviewUrl.value = null;
  }
}

function clearSelectedImage() {
  selectedImage.value = null;
  revokePreviewUrl();
  if (fileInputRef.value) {
    fileInputRef.value.value = '';
  }
}

function onImageChange(event: Event) {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0];
  imageError.value = '';

  if (!file) {
    clearSelectedImage();
    return;
  }

  if (!file.type.startsWith('image/')) {
    imageError.value = '請選擇圖片檔案。';
    clearSelectedImage();
    return;
  }

  if (file.size > MAX_IMAGE_SIZE_BYTES) {
    imageError.value = '圖片大小不可超過 1MB。';
    clearSelectedImage();
    return;
  }

  selectedImage.value = file;
  revokePreviewUrl();
  imagePreviewUrl.value = URL.createObjectURL(file);
}

async function loadPosts() {
  isLoadingPosts.value = true;
  loadingError.value = '';
  try {
    posts.value = await fetchPosts();
  } catch (error) {
    loadingError.value = error instanceof Error ? error.message : '載入貼文失敗。';
  } finally {
    isLoadingPosts.value = false;
  }
}

async function submitPost() {
  const trimmedContent = content.value.trim();
  submitError.value = '';

  if (trimmedContent.length === 0) {
    submitError.value = '請先輸入貼文內容。';
    return;
  }

  if (imageError.value.length > 0) {
    submitError.value = imageError.value;
    return;
  }

  isSubmitting.value = true;
  let uploadedImageObjectKey: string | undefined;

  try {
    if (selectedImage.value) {
      isUploadingImage.value = true;
      uploadedImageObjectKey = await uploadPostImage(selectedImage.value);
    }

    const createdPost = await createPost(
      uploadedImageObjectKey
        ? { content: trimmedContent, imageUrl: uploadedImageObjectKey }
        : { content: trimmedContent }
    );

    posts.value = [createdPost, ...posts.value];
    content.value = '';
    clearSelectedImage();
  } catch (error) {
    submitError.value = error instanceof Error ? error.message : '發佈貼文失敗。';
  } finally {
    isUploadingImage.value = false;
    isSubmitting.value = false;
  }
}

onMounted(() => {
  void loadPosts();
});

onBeforeUnmount(() => {
  revokePreviewUrl();
});
</script>

<template>
  <section class="composer-card">
    <h1>建立貼文</h1>
    <p class="helper-text">圖片上傳上限 1MB，圖片會先送到後端再上傳到儲存空間。</p>

    <textarea
      v-model="content"
      class="content-input"
      placeholder="分享今天的想法..."
      rows="4"
      :disabled="isSubmitting"
    />

    <div class="image-row">
      <input ref="fileInputRef" type="file" accept="image/*" :disabled="isSubmitting" @change="onImageChange" />
      <button v-if="selectedImage" type="button" class="text-btn" :disabled="isSubmitting" @click="clearSelectedImage">
        移除圖片
      </button>
    </div>

    <p v-if="selectedImage" class="file-meta">
      已選擇：{{ selectedImage.name }}（{{ (selectedImage.size / 1024).toFixed(1) }} KB）
    </p>

    <p v-if="imageError" class="error-text">{{ imageError }}</p>
    <p v-if="submitError" class="error-text">{{ submitError }}</p>

    <img v-if="imagePreviewUrl" :src="imagePreviewUrl" alt="preview" class="preview-image" />

    <button class="submit-btn" type="button" :disabled="!canSubmit" @click="submitPost">
      {{ isUploadingImage ? '圖片上傳中…' : isSubmitting ? '發佈中…' : '發佈貼文' }}
    </button>
  </section>

  <section class="feed-card">
    <header class="feed-header">
      <h2>最新貼文</h2>
      <button class="text-btn" type="button" :disabled="isLoadingPosts" @click="loadPosts">重新整理</button>
    </header>

    <p v-if="isLoadingPosts" class="state-text">載入中…</p>
    <p v-else-if="loadingError" class="error-text">{{ loadingError }}</p>
    <p v-else-if="posts.length === 0" class="state-text">目前還沒有貼文。</p>

    <ul v-else class="post-list">
      <li v-for="post in posts" :key="post.id" class="post-item">
        <p class="post-meta">{{ post.userName }} · {{ formatDateTime(post.createdAt) }}</p>
        <p class="post-content">{{ post.content }}</p>
        <img v-if="post.imageUrl" :src="post.imageUrl" alt="post image" class="post-image" />
      </li>
    </ul>
  </section>
</template>

<style scoped>
.composer-card,
.feed-card {
  padding: 1.25rem;
  border-radius: 1rem;
  border: 1px solid rgba(15, 23, 42, 0.1);
  background: rgba(255, 255, 255, 0.9);
}

.composer-card {
  display: grid;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.composer-card h1 {
  margin: 0;
  font-size: 1.25rem;
}

.helper-text {
  margin: 0;
  color: #64748b;
  font-size: 0.9rem;
}

.content-input {
  width: 100%;
  border-radius: 0.75rem;
  border: 1px solid #cbd5e1;
  padding: 0.75rem;
  font: inherit;
  resize: vertical;
}

.image-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.file-meta {
  margin: 0;
  color: #475569;
  font-size: 0.9rem;
}

.preview-image {
  width: min(100%, 400px);
  border-radius: 0.75rem;
  border: 1px solid #cbd5e1;
}

.submit-btn {
  width: fit-content;
  padding: 0.5rem 1rem;
  border: 0;
  border-radius: 999px;
  background: #0f766e;
  color: white;
  font-weight: 600;
  cursor: pointer;
}

.submit-btn:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.feed-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.75rem;
}

.feed-header h2 {
  margin: 0;
  font-size: 1.1rem;
}

.post-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 0.75rem;
}

.post-item {
  padding: 0.9rem;
  border-radius: 0.75rem;
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: white;
}

.post-meta {
  margin: 0 0 0.45rem;
  color: #64748b;
  font-size: 0.85rem;
}

.post-content {
  margin: 0 0 0.5rem;
  color: #0f172a;
  white-space: pre-wrap;
}

.post-image {
  width: min(100%, 420px);
  border-radius: 0.6rem;
  border: 1px solid #e2e8f0;
}

.text-btn {
  border: 0;
  background: transparent;
  color: #0f766e;
  font-size: 0.9rem;
  cursor: pointer;
}

.text-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.state-text {
  margin: 0;
  color: #64748b;
}

.error-text {
  margin: 0;
  color: #b91c1c;
}
</style>
