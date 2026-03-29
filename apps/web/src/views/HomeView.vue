<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { createPost, deletePost, fetchPosts, updatePost, uploadPostImage } from '../services/api';
import { useAuthStore } from '../stores/auth';
import type { PostItem } from '../types';

const MAX_IMAGE_SIZE_BYTES = 1 * 1024 * 1024;
const MAX_POST_CONTENT_LENGTH = 500;

const authStore = useAuthStore();
const currentUserId = computed(() => authStore.currentUser?.id ?? null);

const posts = ref<PostItem[]>([]);
const isLoadingPosts = ref(true);
const loadingError = ref('');

const content = ref('');
const selectedImage = ref<File | null>(null);
const imagePreviewUrl = ref<string | null>(null);
const imageError = ref('');
const submitError = ref('');
const postActionError = ref('');
const postActionPostId = ref<number | null>(null);

const isSubmitting = ref(false);
const isUploadingImage = ref(false);
const fileInputRef = ref<HTMLInputElement | null>(null);
const editingPostId = ref<number | null>(null);
const editDraftContent = ref('');
const isSavingEdit = ref(false);
const deletingPostId = ref<number | null>(null);

const canSubmit = computed(() => {
  const trimmed = content.value.trim();
  return (
    trimmed.length > 0 &&
    trimmed.length <= MAX_POST_CONTENT_LENGTH &&
    !isSubmitting.value &&
    imageError.value.length === 0
  );
});

function formatDateTime(dateTime: string): string {
  return new Date(dateTime).toLocaleString();
}

function isPostOwner(post: PostItem): boolean {
  return currentUserId.value !== null && post.userId === currentUserId.value;
}

function replacePost(nextPost: PostItem) {
  posts.value = posts.value.map((post) => (post.id === nextPost.id ? nextPost : post));
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
  postActionError.value = '';
  postActionPostId.value = null;
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
  if (trimmedContent.length > MAX_POST_CONTENT_LENGTH) {
    submitError.value = `貼文內容不可超過 ${MAX_POST_CONTENT_LENGTH} 字。`;
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

function beginEditPost(post: PostItem) {
  editingPostId.value = post.id;
  editDraftContent.value = post.content;
  postActionError.value = '';
  postActionPostId.value = post.id;
}

function cancelEditPost() {
  editingPostId.value = null;
  editDraftContent.value = '';
  postActionError.value = '';
  postActionPostId.value = null;
}

async function saveEditPost(post: PostItem) {
  if (isSavingEdit.value) {
    return;
  }

  const trimmed = editDraftContent.value.trim();
  if (trimmed.length === 0) {
    postActionError.value = '貼文內容不可為空。';
    postActionPostId.value = post.id;
    return;
  }
  if (trimmed.length > MAX_POST_CONTENT_LENGTH) {
    postActionError.value = `貼文內容不可超過 ${MAX_POST_CONTENT_LENGTH} 字。`;
    postActionPostId.value = post.id;
    return;
  }

  isSavingEdit.value = true;
  postActionError.value = '';
  try {
    const updated = await updatePost(post.id, { content: trimmed });
    replacePost(updated);
    cancelEditPost();
  } catch (error) {
    postActionError.value = error instanceof Error ? error.message : '更新貼文失敗。';
    postActionPostId.value = post.id;
  } finally {
    isSavingEdit.value = false;
  }
}

async function removePost(post: PostItem) {
  if (deletingPostId.value !== null) {
    return;
  }

  const confirmed = window.confirm('確定要刪除這篇貼文嗎？此操作無法復原。');
  if (!confirmed) {
    return;
  }

  deletingPostId.value = post.id;
  postActionError.value = '';
  postActionPostId.value = post.id;
  try {
    await deletePost(post.id);
    posts.value = posts.value.filter((candidate) => candidate.id !== post.id);
    if (editingPostId.value === post.id) {
      cancelEditPost();
    }
    postActionPostId.value = null;
  } catch (error) {
    postActionError.value = error instanceof Error ? error.message : '刪除貼文失敗。';
  } finally {
    deletingPostId.value = null;
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

    <textarea
      v-model="content"
      class="content-input"
      placeholder="分享今天的想法..."
      rows="4"
      :disabled="isSubmitting"
    />

    <!-- Image upload zone -->
    <input
      ref="fileInputRef"
      type="file"
      accept="image/*"
      class="file-input-hidden"
      :disabled="isSubmitting"
      @change="onImageChange"
    />

    <div
      v-if="!selectedImage"
      class="upload-trigger"
      :class="{ 'is-disabled': isSubmitting }"
      @click="fileInputRef?.click()"
    >
      <span class="upload-icon">🖼</span>
      <span>新增圖片（上限 1&nbsp;MB）</span>
    </div>

    <div v-else class="image-preview-wrap">
      <img :src="imagePreviewUrl!" alt="preview" class="preview-image" />
      <div class="image-preview-meta">
        <span class="file-meta">{{ selectedImage.name }} · {{ (selectedImage.size / 1024).toFixed(1) }} KB</span>
        <button type="button" class="remove-image-btn" :disabled="isSubmitting" @click="clearSelectedImage">
          移除
        </button>
      </div>
    </div>

    <p v-if="imageError" class="error-text">{{ imageError }}</p>
    <p v-if="submitError" class="error-text">{{ submitError }}</p>

    <button class="post-submit-btn" type="button" :disabled="!canSubmit" @click="submitPost">
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
        <p class="post-meta">
          {{ post.userName }} · {{ formatDateTime(post.createdAt) }}
          <span v-if="post.updatedAt" class="post-updated">（已編輯 {{ formatDateTime(post.updatedAt) }}）</span>
        </p>

        <div v-if="editingPostId === post.id" class="edit-post-panel">
          <textarea
            v-model="editDraftContent"
            class="content-input edit-content-input"
            rows="4"
            :maxlength="MAX_POST_CONTENT_LENGTH"
            :disabled="isSavingEdit"
          />
          <div class="edit-post-footer">
            <span
              class="edit-char-count"
              :class="{ 'is-warning': editDraftContent.trim().length > MAX_POST_CONTENT_LENGTH }"
            >
              {{ editDraftContent.trim().length }} / {{ MAX_POST_CONTENT_LENGTH }}
            </span>
            <div class="edit-post-actions">
              <button type="button" class="secondary-btn" :disabled="isSavingEdit" @click="cancelEditPost">取消</button>
              <button type="button" class="post-submit-btn" :disabled="isSavingEdit" @click="saveEditPost(post)">
                {{ isSavingEdit ? '儲存中…' : '儲存' }}
              </button>
            </div>
          </div>
        </div>
        <p v-else class="post-content">{{ post.content }}</p>

        <img v-if="post.imageUrl" :src="post.imageUrl" alt="post image" class="post-image" />
        <div class="post-actions">
          <RouterLink class="text-btn" :to="{ name: 'post-detail', params: { postId: post.id } }">
            查看討論串
          </RouterLink>
          <template v-if="isPostOwner(post)">
            <button
              v-if="editingPostId !== post.id"
              type="button"
              class="text-btn"
              :disabled="deletingPostId === post.id"
              @click="beginEditPost(post)"
            >
              編輯
            </button>
            <button
              type="button"
              class="text-btn danger-text-btn"
              :disabled="isSavingEdit || deletingPostId === post.id"
              @click="removePost(post)"
            >
              {{ deletingPostId === post.id ? '刪除中…' : '刪除' }}
            </button>
          </template>
        </div>
        <p v-if="postActionError && postActionPostId === post.id" class="error-text post-action-error">
          {{ postActionError }}
        </p>
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

.content-input {
  width: 100%;
  border-radius: 0.75rem;
  border: 1px solid #cbd5e1;
  padding: 0.75rem;
  font: inherit;
  resize: vertical;
  transition:
    border-color 0.15s,
    box-shadow 0.15s;
}

.content-input:focus {
  outline: none;
  border-color: #0f766e;
  box-shadow: 0 0 0 3px rgba(15, 118, 110, 0.12);
}

/* Hidden native file input */
.file-input-hidden {
  display: none;
}

/* Styled upload trigger */
.upload-trigger {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.6rem 0.9rem;
  border-radius: 0.65rem;
  border: 1.5px dashed #94a3b8;
  color: #475569;
  font-size: 0.9rem;
  cursor: pointer;
  transition:
    border-color 0.15s,
    background 0.15s;
  user-select: none;
  width: fit-content;
}

.upload-trigger:hover:not(.is-disabled) {
  border-color: #0f766e;
  background: #f0fdfa;
  color: #0f766e;
}

.upload-trigger.is-disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.upload-icon {
  font-size: 1.1rem;
  line-height: 1;
}

/* Preview */
.image-preview-wrap {
  display: grid;
  gap: 0.45rem;
}

.preview-image {
  width: min(100%, 400px);
  border-radius: 0.75rem;
  border: 1px solid #cbd5e1;
  display: block;
}

.image-preview-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}

.file-meta {
  color: #475569;
  font-size: 0.875rem;
}

.remove-image-btn {
  border: 0;
  background: transparent;
  color: #dc2626;
  font-size: 0.875rem;
  cursor: pointer;
  padding: 0;
}

.remove-image-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.post-submit-btn {
  width: fit-content;
  padding: 0.5rem 1.25rem;
  border: 0;
  border-radius: 999px;
  background: linear-gradient(135deg, #0f766e, #0d9488);
  color: white;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.15s;
}

.post-submit-btn:disabled {
  cursor: not-allowed;
  opacity: 0.5;
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

.post-updated {
  color: #94a3b8;
}

.post-content {
  margin: 0 0 0.5rem;
  color: #0f172a;
  white-space: pre-wrap;
}

.edit-post-panel {
  display: grid;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
}

.edit-content-input {
  margin: 0;
}

.edit-post-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.edit-char-count {
  font-size: 0.8rem;
  color: #64748b;
}

.edit-char-count.is-warning {
  color: #b45309;
}

.edit-post-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.secondary-btn {
  padding: 0.45rem 1rem;
  border: 1px solid rgba(148, 163, 184, 0.45);
  border-radius: 999px;
  background: white;
  color: #334155;
  font-weight: 600;
  cursor: pointer;
}

.secondary-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.post-image {
  display: block;
  width: min(100%, 420px);
  border-radius: 0.6rem;
  border: 1px solid #e2e8f0;
  margin-bottom: 0.5rem;
}

.post-actions {
  margin-top: 0.5rem;
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.text-btn {
  border: 0;
  background: transparent;
  color: #0f766e;
  font-size: 0.9rem;
  cursor: pointer;
  padding: 0;
  text-decoration: none;
}

.text-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.danger-text-btn {
  color: #dc2626;
}

.post-action-error {
  margin-top: 0.35rem;
}

.state-text {
  margin: 0;
  color: #64748b;
}

.error-text {
  margin: 0;
  color: #b91c1c;
  font-size: 0.875rem;
}
</style>
