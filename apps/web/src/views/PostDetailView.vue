<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import CommentThreadNode from '../components/comment/CommentThreadNode.vue';
import {
  createComment,
  deletePost,
  deleteComment,
  fetchCommentPage,
  fetchPost,
  updateComment,
  updatePost,
} from '../services/api';
import { useAuthStore } from '../stores/auth';
import type { CommentCreatedEvent, CommentItem, PostItem } from '../types';
import { resolveSseUrl } from '@simple-social-media/utils';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const MAX_POST_CONTENT_LENGTH = 500;
const COMMENT_PAGE_SIZE = 5;
const DEFAULT_PAGE_STATE = {
  loaded: false,
  hasMore: true,
  loading: false,
  nextOffset: 0,
};

const post = ref<PostItem | null>(null);
const commentsById = ref<Record<number, CommentItem>>({});
const childIdsByParent = ref<Record<string, number[]>>({ root: [] });
const pageStateByParent = ref<
  Record<string, { loaded: boolean; hasMore: boolean; loading: boolean; nextOffset: number }>
>({
  root: { ...DEFAULT_PAGE_STATE },
});
const commentDraft = ref('');
const replyTargetId = ref<number | null>(null);
const errorMessage = ref('');
const isSubmitting = ref(false);
const deletingCommentId = ref<number | null>(null);
const editingCommentId = ref<number | null>(null);
const isUpdatingComment = ref(false);
const isEditingPost = ref(false);
const editPostDraft = ref('');
const isSavingPost = ref(false);
const isDeletingPost = ref(false);
const postActionError = ref('');

const postId = computed(() => Number(route.params.postId));
const currentUserId = computed(() => authStore.currentUser?.id ?? null);
const isPostOwner = computed(() => {
  return post.value !== null && currentUserId.value !== null && post.value.userId === currentUserId.value;
});

const childrenByParent = computed<Record<string, CommentItem[]>>(() => {
  const grouped: Record<string, CommentItem[]> = {};
  for (const [parentKey, commentIds] of Object.entries(childIdsByParent.value)) {
    grouped[parentKey] = commentIds
      .map((commentId) => commentsById.value[commentId])
      .filter((comment): comment is CommentItem => comment !== undefined);
  }
  if (!grouped.root) {
    grouped.root = [];
  }
  return grouped;
});

const loadedCommentCount = computed(() => Object.keys(commentsById.value).length);
const rootComments = computed(() => childrenByParent.value.root ?? []);
const rootPageState = computed(() => {
  return pageStateByParent.value.root ?? { ...DEFAULT_PAGE_STATE };
});
const hasMoreRootComments = computed(() => rootPageState.value.hasMore);
const isLoadingRootComments = computed(() => rootPageState.value.loading);

let stream: EventSource | null = null;

function parentKey(parentCommentId: number | null): string {
  return parentCommentId === null ? 'root' : String(parentCommentId);
}

function readPageState(key: string) {
  return pageStateByParent.value[key] ?? { ...DEFAULT_PAGE_STATE };
}

function writePageState(
  key: string,
  nextState: { loaded: boolean; hasMore: boolean; loading: boolean; nextOffset: number }
) {
  pageStateByParent.value = {
    ...pageStateByParent.value,
    [key]: nextState,
  };
}

function mergeComment(nextComment: CommentItem, forceVisible = false) {
  commentsById.value = {
    ...commentsById.value,
    [nextComment.id]: nextComment,
  };

  const key = parentKey(nextComment.parentCommentId ?? null);
  const state = readPageState(key);
  const shouldAttach = forceVisible || key === 'root' || state.loaded;
  if (!shouldAttach) {
    return;
  }

  const existingIds = childIdsByParent.value[key] ?? [];
  if (existingIds.includes(nextComment.id)) {
    return;
  }
  childIdsByParent.value = {
    ...childIdsByParent.value,
    [key]: [...existingIds, nextComment.id],
  };
}

function resetThreadState() {
  commentsById.value = {};
  childIdsByParent.value = { root: [] };
  pageStateByParent.value = { root: { ...DEFAULT_PAGE_STATE } };
}

async function loadCommentPage(parentCommentId: number | null, options?: { reset?: boolean }) {
  const reset = options?.reset === true;
  const key = parentKey(parentCommentId);
  const currentState = readPageState(key);

  if (currentState.loading) {
    return;
  }
  if (!reset && currentState.loaded && !currentState.hasMore) {
    return;
  }

  const offset = reset ? 0 : currentState.nextOffset;
  writePageState(key, {
    ...currentState,
    loading: true,
    nextOffset: offset,
  });

  try {
    const page = await fetchCommentPage(postId.value, {
      parentCommentId,
      offset,
      limit: COMMENT_PAGE_SIZE,
    });

    const nextCommentsById = { ...commentsById.value };
    const nextIds = reset ? [] : [...(childIdsByParent.value[key] ?? [])];
    for (const comment of page.comments) {
      nextCommentsById[comment.id] = comment;
      if (!nextIds.includes(comment.id)) {
        nextIds.push(comment.id);
      }
    }

    commentsById.value = nextCommentsById;
    childIdsByParent.value = {
      ...childIdsByParent.value,
      [key]: nextIds,
    };
    writePageState(key, {
      loaded: true,
      hasMore: page.hasMore,
      loading: false,
      nextOffset: page.nextOffset ?? offset + page.comments.length,
    });
  } catch (error) {
    writePageState(key, {
      ...currentState,
      loading: false,
    });
    throw error;
  }
}

function loadMoreReplies(parentCommentId: number | null) {
  void loadCommentPage(parentCommentId).catch((error) => {
    console.error(error);
    errorMessage.value =
      error instanceof Error ? error.message : 'Comment feed is unavailable until the backend is running.';
  });
}

async function loadPost() {
  try {
    post.value = await fetchPost(postId.value);
  } catch (error) {
    console.error(error);
    errorMessage.value = 'Post detail could not be loaded.';
  }
}

function beginPostEdit() {
  if (!post.value) {
    return;
  }

  isEditingPost.value = true;
  editPostDraft.value = post.value.content;
  postActionError.value = '';
}

function cancelPostEdit() {
  isEditingPost.value = false;
  editPostDraft.value = '';
  postActionError.value = '';
}

async function savePostEdit() {
  if (!post.value || isSavingPost.value) {
    return;
  }

  const trimmed = editPostDraft.value.trim();
  if (trimmed.length === 0) {
    postActionError.value = '貼文內容不可為空。';
    return;
  }
  if (trimmed.length > MAX_POST_CONTENT_LENGTH) {
    postActionError.value = `貼文內容不可超過 ${MAX_POST_CONTENT_LENGTH} 字。`;
    return;
  }

  isSavingPost.value = true;
  postActionError.value = '';
  try {
    post.value = await updatePost(postId.value, { content: trimmed });
    cancelPostEdit();
  } catch (error) {
    postActionError.value = error instanceof Error ? error.message : '更新貼文失敗。';
  } finally {
    isSavingPost.value = false;
  }
}

async function removePost() {
  if (isDeletingPost.value) {
    return;
  }

  const confirmed = window.confirm('確定要刪除這篇貼文嗎？此操作無法復原。');
  if (!confirmed) {
    return;
  }

  isDeletingPost.value = true;
  postActionError.value = '';
  try {
    await deletePost(postId.value);
    await router.push('/');
  } catch (error) {
    postActionError.value = error instanceof Error ? error.message : '刪除貼文失敗。';
  } finally {
    isDeletingPost.value = false;
  }
}

function connectStream() {
  stream?.close();
  stream = new EventSource(resolveSseUrl(`/api/posts/${postId.value}/comments/stream`), {
    withCredentials: true,
  });

  stream.addEventListener('comment.created', (event) => {
    const payload = JSON.parse((event as MessageEvent<string>).data) as CommentCreatedEvent;
    if (payload.postId !== postId.value) {
      return;
    }
    mergeComment(payload.comment, true);
  });

  stream.onerror = () => {
    void loadCommentPage(null, { reset: true }).catch((error) => {
      console.error(error);
      errorMessage.value = 'Comment feed is unavailable until the backend is running.';
    });
  };
}

async function submitComment() {
  if (!commentDraft.value.trim()) {
    return;
  }

  isSubmitting.value = true;
  try {
    const created = await createComment(postId.value, {
      content: commentDraft.value.trim(),
      parentCommentId: null,
    });
    mergeComment(created, true);
    commentDraft.value = '';
  } catch (error) {
    console.error(error);
    errorMessage.value = error instanceof Error ? error.message : 'Comment creation failed.';
  } finally {
    isSubmitting.value = false;
  }
}

async function submitReply(parentCommentId: number, content: string) {
  try {
    const created = await createComment(postId.value, { content, parentCommentId });
    mergeComment(created, true);
    replyTargetId.value = null;
  } catch (error) {
    console.error(error);
    errorMessage.value = error instanceof Error ? error.message : 'Reply failed.';
  }
}

async function removeComment(commentId: number) {
  if (deletingCommentId.value !== null) {
    return;
  }

  deletingCommentId.value = commentId;
  try {
    const deletedComment = await deleteComment(postId.value, commentId);
    mergeComment(deletedComment, true);
    if (replyTargetId.value === commentId) {
      replyTargetId.value = null;
    }
    if (editingCommentId.value === commentId) {
      editingCommentId.value = null;
    }
  } catch (error) {
    console.error(error);
    errorMessage.value = error instanceof Error ? error.message : 'Delete comment failed.';
  } finally {
    deletingCommentId.value = null;
  }
}

function startEditComment(commentId: number) {
  editingCommentId.value = commentId;
}

function cancelEditComment() {
  editingCommentId.value = null;
}

async function submitEditComment(commentId: number, content: string) {
  if (isUpdatingComment.value) {
    return;
  }

  isUpdatingComment.value = true;
  try {
    const updated = await updateComment(postId.value, commentId, { content });
    mergeComment(updated, true);
    editingCommentId.value = null;
  } catch (error) {
    console.error(error);
    errorMessage.value = error instanceof Error ? error.message : 'Edit comment failed.';
  } finally {
    isUpdatingComment.value = false;
  }
}

function startReply(commentId: number) {
  replyTargetId.value = commentId;
}

function cancelReply() {
  replyTargetId.value = null;
}

watch(
  postId,
  () => {
    errorMessage.value = '';
    replyTargetId.value = null;
    editingCommentId.value = null;
    isEditingPost.value = false;
    editPostDraft.value = '';
    postActionError.value = '';
    resetThreadState();
    void loadPost();
    void loadCommentPage(null, { reset: true }).catch((error) => {
      console.error(error);
      errorMessage.value = 'Comment feed is unavailable until the backend is running.';
    });
    connectStream();
  },
  { immediate: true }
);

onBeforeUnmount(() => {
  stream?.close();
});
</script>

<template>
  <section class="detail-card">
    <div class="post-header-row">
      <p class="detail-label">Post detail</p>
      <div v-if="isPostOwner" class="post-owner-actions">
        <button v-if="!isEditingPost" type="button" class="text-btn" :disabled="isDeletingPost" @click="beginPostEdit">
          編輯貼文
        </button>
        <button
          type="button"
          class="text-btn danger-text-btn"
          :disabled="isSavingPost || isDeletingPost"
          @click="removePost"
        >
          {{ isDeletingPost ? '刪除中…' : '刪除貼文' }}
        </button>
      </div>
    </div>
    <p v-if="post" class="meta">
      {{ post.userName }} · {{ new Date(post.createdAt).toLocaleString() }}
      <span v-if="post.updatedAt">（已編輯 {{ new Date(post.updatedAt).toLocaleString() }}）</span>
    </p>

    <div v-if="isEditingPost" class="edit-post-panel">
      <textarea v-model="editPostDraft" rows="4" :maxlength="MAX_POST_CONTENT_LENGTH" class="edit-post-textarea" />
      <div class="edit-post-footer">
        <span class="char-count" :class="{ 'near-limit': editPostDraft.trim().length > MAX_POST_CONTENT_LENGTH }">
          {{ editPostDraft.trim().length }} / {{ MAX_POST_CONTENT_LENGTH }}
        </span>
        <div class="edit-post-actions">
          <button type="button" class="text-btn" :disabled="isSavingPost" @click="cancelPostEdit">取消</button>
          <button type="button" class="comment-button" :disabled="isSavingPost" @click="savePostEdit">
            {{ isSavingPost ? '儲存中…' : '儲存' }}
          </button>
        </div>
      </div>
    </div>
    <p v-else class="post-body">{{ post?.content ?? 'Loading post…' }}</p>

    <img v-if="post?.imageUrl" :src="post.imageUrl" alt="post image" class="post-detail-image" />
    <p v-if="postActionError" class="error">{{ postActionError }}</p>
    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
  </section>

  <section class="detail-card">
    <div class="comments-header">
      <div>
        <p class="detail-label">Realtime comments</p>
        <h2>{{ loadedCommentCount }} loaded comment(s)</h2>
      </div>
      <span class="stream-pill">SSE stream open</span>
    </div>

    <div v-if="authStore.isAuthenticated" class="comment-composer">
      <textarea v-model="commentDraft" rows="4" maxlength="280" placeholder="Join the conversation…" />
      <div class="composer-footer">
        <span class="char-count" :class="{ 'near-limit': commentDraft.length >= 260 }"
          >{{ commentDraft.length }}&thinsp;/&thinsp;280</span
        >
        <button :disabled="isSubmitting" class="comment-button" @click="submitComment">
          {{ isSubmitting ? 'Sending…' : 'Send comment' }}
        </button>
      </div>
    </div>

    <p v-if="rootComments.length === 0" class="empty-state">No comments yet.</p>

    <CommentThreadNode
      v-for="comment in rootComments"
      :key="comment.id"
      :comment="comment"
      :depth="0"
      :children-by-parent="childrenByParent"
      :page-state-by-parent="pageStateByParent"
      :can-reply="authStore.isAuthenticated"
      :current-user-id="currentUserId"
      :active-reply-id="replyTargetId"
      :active-edit-id="editingCommentId"
      :is-updating-comment="isUpdatingComment"
      @reply="startReply"
      @cancel-reply="cancelReply"
      @submit-reply="submitReply"
      @start-edit="startEditComment"
      @cancel-edit="cancelEditComment"
      @submit-edit="submitEditComment"
      @delete="removeComment"
      @load-more="loadMoreReplies"
    />

    <button
      v-if="hasMoreRootComments || isLoadingRootComments"
      type="button"
      class="load-more-root"
      :disabled="isLoadingRootComments"
      @click="loadMoreReplies(null)"
    >
      {{ isLoadingRootComments ? '載入中…' : 'Load more comments' }}
    </button>
  </section>
</template>

<style scoped>
.detail-card {
  margin-bottom: 1.5rem;
  padding: 1.5rem;
  border-radius: 1.5rem;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(15, 23, 42, 0.08);
}

.detail-label {
  margin: 0 0 0.5rem;
  text-transform: uppercase;
  letter-spacing: 0.14em;
  font-size: 0.75rem;
  color: #0f766e;
}

.post-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.post-owner-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.post-body {
  margin: 0.25rem 0 0.75rem;
  white-space: pre-wrap;
  color: #0f172a;
  font-size: 1.05rem;
  line-height: 1.6;
}

.post-detail-image {
  display: block;
  width: min(100%, 560px);
  border-radius: 0.85rem;
  border: 1px solid #e2e8f0;
  margin-bottom: 0.5rem;
}

.meta {
  margin: 0 0 0.35rem;
  color: #475569;
  font-size: 0.875rem;
}

.edit-post-panel {
  margin: 0.35rem 0 0.75rem;
}

.edit-post-textarea {
  width: 100%;
  resize: vertical;
  padding: 1rem;
  border-radius: 1rem;
  border: 1px solid rgba(148, 163, 184, 0.35);
  font: inherit;
}

.edit-post-footer {
  margin-top: 0.6rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.edit-post-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.comments-header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
}

.stream-pill {
  padding: 0.5rem 0.85rem;
  border-radius: 999px;
  background: #ecfeff;
  color: #0f766e;
  font-weight: 600;
}

.comment-composer {
  margin: 1.25rem 0 1.5rem;
}

textarea {
  width: 100%;
  resize: vertical;
  padding: 1rem;
  border-radius: 1rem;
  border: 1px solid rgba(148, 163, 184, 0.35);
  font: inherit;
  transition:
    border-color 0.15s,
    box-shadow 0.15s;
}

textarea:focus {
  outline: none;
  border-color: #0f766e;
  box-shadow: 0 0 0 3px rgba(15, 118, 110, 0.1);
}

.composer-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 0.75rem;
  gap: 0.75rem;
}

.char-count {
  font-size: 0.8rem;
  color: #94a3b8;
}

.char-count.near-limit {
  color: #f59e0b;
}

.comment-button {
  padding: 0.65rem 1.25rem;
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, #0f766e, #0d9488);
  color: white;
  font-weight: 700;
  cursor: pointer;
  transition: opacity 0.15s;
}

.comment-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.load-more-root {
  margin-top: 1rem;
  border: 0;
  background: transparent;
  color: #0f766e;
  cursor: pointer;
  font-weight: 700;
}

.load-more-root:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.empty-state {
  color: #64748b;
}

.error {
  color: #b91c1c;
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
</style>
