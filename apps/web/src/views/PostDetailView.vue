<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import CommentThreadNode from '../components/comment/CommentThreadNode.vue';
import { createComment, deleteComment, fetchComments, fetchPost } from '../services/api';
import { useAuthStore } from '../stores/auth';
import type { CommentCreatedEvent, CommentItem, PostItem } from '../types';
import { resolveSseUrl } from '@simple-social-media/utils';

const route = useRoute();
const authStore = useAuthStore();

const post = ref<PostItem | null>(null);
const comments = ref<CommentItem[]>([]);
const commentDraft = ref('');
const replyTargetId = ref<number | null>(null);
const visibleCountByParent = ref<Record<string, number>>({ root: 5 });
const errorMessage = ref('');
const isSubmitting = ref(false);
const deletingCommentId = ref<number | null>(null);

const postId = computed(() => Number(route.params.postId));
const currentUserId = computed(() => authStore.currentUser?.id ?? null);

const sortedComments = computed(() => {
  return [...comments.value].sort((a, b) => {
    const createdAtCompare = new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime();
    return createdAtCompare !== 0 ? createdAtCompare : a.id - b.id;
  });
});

const childrenByParent = computed<Record<string, CommentItem[]>>(() => {
  const grouped: Record<string, CommentItem[]> = { root: [] };
  const existingIds = new Set(sortedComments.value.map((comment) => comment.id));

  for (const comment of sortedComments.value) {
    const parentId = comment.parentCommentId ?? null;
    const resolvedParentId = parentId !== null && existingIds.has(parentId) ? parentId : null;
    const parentKey = resolvedParentId === null ? 'root' : String(resolvedParentId);

    if (!grouped[parentKey]) {
      grouped[parentKey] = [];
    }
    grouped[parentKey].push(comment);
  }

  return grouped;
});

const rootVisibleCount = computed(() => visibleCountByParent.value.root ?? 5);
const rootComments = computed(() => (childrenByParent.value.root ?? []).slice(0, rootVisibleCount.value));
const hasMoreRootComments = computed(() => (childrenByParent.value.root?.length ?? 0) > rootVisibleCount.value);
const remainingRootComments = computed(() => (childrenByParent.value.root?.length ?? 0) - rootVisibleCount.value);

watch(
  childrenByParent,
  (grouped) => {
    const next = { ...visibleCountByParent.value };
    let changed = false;

    if (next.root === undefined) {
      next.root = 5;
      changed = true;
    }

    for (const parentKey of Object.keys(grouped)) {
      if (next[parentKey] === undefined) {
        next[parentKey] = 5;
        changed = true;
      }
    }

    if (changed) {
      visibleCountByParent.value = next;
    }
  },
  { immediate: true }
);

let stream: EventSource | null = null;

function upsertComment(nextComment: CommentItem) {
  const existingIndex = comments.value.findIndex((comment) => comment.id === nextComment.id);
  if (existingIndex >= 0) {
    const next = [...comments.value];
    next[existingIndex] = nextComment;
    comments.value = next;
    return;
  }
  comments.value = [...comments.value, nextComment];
}

function resetVisibleCounts() {
  visibleCountByParent.value = { root: 5 };
}

function loadMoreReplies(parentCommentId: number | null) {
  const parentKey = parentCommentId === null ? 'root' : String(parentCommentId);
  const current = visibleCountByParent.value[parentKey] ?? 5;
  visibleCountByParent.value = {
    ...visibleCountByParent.value,
    [parentKey]: current + 5,
  };
}

async function loadPost() {
  try {
    post.value = await fetchPost(postId.value);
  } catch (error) {
    console.error(error);
    errorMessage.value = 'Post detail could not be loaded.';
  }
}

async function loadComments() {
  try {
    comments.value = await fetchComments(postId.value);
  } catch (error) {
    console.error(error);
    errorMessage.value = 'Comment feed is unavailable until the backend is running.';
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
    upsertComment(payload.comment);
  });

  stream.onerror = () => {
    void loadComments();
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
    upsertComment(created);
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
    upsertComment(created);
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
    upsertComment(deletedComment);
    if (replyTargetId.value === commentId) {
      replyTargetId.value = null;
    }
  } catch (error) {
    console.error(error);
    errorMessage.value = error instanceof Error ? error.message : 'Delete comment failed.';
  } finally {
    deletingCommentId.value = null;
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
    resetVisibleCounts();
    void loadPost();
    void loadComments();
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
    <p class="detail-label">Post detail</p>
    <p v-if="post" class="meta">{{ post.userName }} · {{ new Date(post.createdAt).toLocaleString() }}</p>
    <p class="post-body">{{ post?.content ?? 'Loading post…' }}</p>
    <img v-if="post?.imageUrl" :src="post.imageUrl" alt="post image" class="post-detail-image" />
    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
  </section>

  <section class="detail-card">
    <div class="comments-header">
      <div>
        <p class="detail-label">Realtime comments</p>
        <h2>{{ comments.length }} comment(s)</h2>
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
      :visible-count-by-parent="visibleCountByParent"
      :can-reply="authStore.isAuthenticated"
      :current-user-id="currentUserId"
      :active-reply-id="replyTargetId"
      @reply="startReply"
      @cancel-reply="cancelReply"
      @submit-reply="submitReply"
      @delete="removeComment"
      @load-more="loadMoreReplies"
    />

    <button v-if="hasMoreRootComments" type="button" class="load-more-root" @click="loadMoreReplies(null)">
      Load more comments ({{ remainingRootComments }})
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

.empty-state {
  color: #64748b;
}

.error {
  color: #b91c1c;
}
</style>
