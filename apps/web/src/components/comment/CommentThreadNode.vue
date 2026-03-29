<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import type { CommentItem } from '../../types';

defineOptions({
  name: 'CommentThreadNode',
});

const props = defineProps<{
  comment: CommentItem;
  depth: number;
  childrenByParent: Record<string, CommentItem[]>;
  visibleCountByParent: Record<string, number>;
  canReply: boolean;
  currentUserId: number | null;
  activeReplyId: number | null;
  activeEditId: number | null;
  isUpdatingComment: boolean;
}>();

const emit = defineEmits<{
  reply: [commentId: number];
  cancelReply: [];
  submitReply: [commentId: number, content: string];
  startEdit: [commentId: number];
  cancelEdit: [];
  submitEdit: [commentId: number, content: string];
  delete: [commentId: number];
  loadMore: [parentCommentId: number];
}>();

const childKey = computed(() => String(props.comment.id));
const allChildren = computed(() => props.childrenByParent[childKey.value] ?? []);
const visibleCount = computed(() => props.visibleCountByParent[childKey.value] ?? 5);
const visibleChildren = computed(() => allChildren.value.slice(0, visibleCount.value));
const hasMoreChildren = computed(() => allChildren.value.length > visibleCount.value);
const remainingChildren = computed(() => allChildren.value.length - visibleCount.value);
const isOwnComment = computed(() => props.currentUserId !== null && props.comment.userId === props.currentUserId);
const isReplying = computed(() => props.activeReplyId === props.comment.id);
const isEditing = computed(() => props.activeEditId === props.comment.id);

const replyDraft = ref('');
const editDraft = ref('');

// Clear draft when this composer is dismissed
watch(isReplying, (active) => {
  if (!active) replyDraft.value = '';
});

watch(isEditing, (active) => {
  if (active) {
    editDraft.value = props.comment.content;
    return;
  }
  editDraft.value = '';
});

function onSubmitReply() {
  const text = replyDraft.value.trim();
  if (!text) return;
  emit('submitReply', props.comment.id, text);
}

function onSubmitEdit() {
  const text = editDraft.value.trim();
  if (!text) return;
  emit('submitEdit', props.comment.id, text);
}
</script>

<template>
  <article class="comment-node" :class="{ 'is-deleted': comment.deleted, 'is-nested': depth > 0 }">
    <div class="comment-head">
      <strong class="comment-author">{{ comment.userName }}</strong>
      <time class="comment-time">{{ new Date(comment.createdAt).toLocaleString() }}</time>
    </div>

    <p class="comment-body">
      <template v-if="comment.deleted"><em>此留言已刪除</em></template>
      <template v-else>{{ comment.content }}</template>
    </p>

    <div v-if="!comment.deleted" class="comment-actions">
      <button
        v-if="canReply && !isReplying"
        type="button"
        class="action-btn reply-btn"
        @click="emit('reply', comment.id)"
      >
        回覆
      </button>
      <button
        v-if="isOwnComment && !isEditing"
        type="button"
        class="action-btn reply-btn"
        @click="emit('startEdit', comment.id)"
      >
        編輯
      </button>
      <button v-if="isOwnComment" type="button" class="action-btn delete-btn" @click="emit('delete', comment.id)">
        刪除
      </button>
    </div>

    <!-- Inline reply composer -->
    <div v-if="isReplying" class="inline-composer">
      <textarea
        v-model="replyDraft"
        class="reply-textarea"
        rows="3"
        maxlength="280"
        :placeholder="`回覆 ${comment.userName}…`"
        autofocus
      />
      <div class="inline-composer-footer">
        <span class="char-count" :class="{ 'near-limit': replyDraft.length >= 260 }">
          {{ replyDraft.length }}&thinsp;/&thinsp;280
        </span>
        <div class="inline-composer-actions">
          <button type="button" class="action-btn cancel-btn" @click="emit('cancelReply')">取消</button>
          <button
            type="button"
            class="reply-submit-btn"
            :disabled="replyDraft.trim().length === 0"
            @click="onSubmitReply"
          >
            送出
          </button>
        </div>
      </div>
    </div>

    <div v-if="isEditing" class="inline-composer">
      <textarea
        v-model="editDraft"
        class="reply-textarea"
        rows="3"
        maxlength="280"
        :placeholder="`編輯 ${comment.userName} 的留言…`"
        autofocus
      />
      <div class="inline-composer-footer">
        <span class="char-count" :class="{ 'near-limit': editDraft.length >= 260 }">
          {{ editDraft.length }}&thinsp;/&thinsp;280
        </span>
        <div class="inline-composer-actions">
          <button type="button" class="action-btn cancel-btn" :disabled="isUpdatingComment" @click="emit('cancelEdit')">
            取消
          </button>
          <button
            type="button"
            class="reply-submit-btn"
            :disabled="isUpdatingComment || editDraft.trim().length === 0"
            @click="onSubmitEdit"
          >
            {{ isUpdatingComment ? '儲存中…' : '儲存' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="visibleChildren.length > 0" class="children">
      <CommentThreadNode
        v-for="child in visibleChildren"
        :key="child.id"
        :comment="child"
        :depth="depth + 1"
        :children-by-parent="childrenByParent"
        :visible-count-by-parent="visibleCountByParent"
        :can-reply="canReply"
        :current-user-id="currentUserId"
        :active-reply-id="activeReplyId"
        :active-edit-id="activeEditId"
        :is-updating-comment="isUpdatingComment"
        @reply="emit('reply', $event)"
        @cancel-reply="emit('cancelReply')"
        @submit-reply="(id, text) => emit('submitReply', id, text)"
        @start-edit="emit('startEdit', $event)"
        @cancel-edit="emit('cancelEdit')"
        @submit-edit="(id, text) => emit('submitEdit', id, text)"
        @delete="emit('delete', $event)"
        @load-more="emit('loadMore', $event)"
      />
      <button
        v-if="hasMoreChildren"
        type="button"
        class="action-btn load-more-btn"
        @click="emit('loadMore', comment.id)"
      >
        顯示更多回覆（{{ remainingChildren }}）
      </button>
    </div>
  </article>
</template>

<style scoped>
.comment-node {
  margin-top: 0.75rem;
  padding: 0.85rem 1rem;
  border-radius: 0.85rem;
  border: 1px solid rgba(148, 163, 184, 0.25);
  background: #f8fafc;
}

.comment-node.is-nested {
  border-left: 3px solid rgba(15, 118, 110, 0.2);
  border-radius: 0 0.85rem 0.85rem 0;
}

.comment-node.is-deleted {
  background: #f1f5f9;
  border-style: dashed;
  border-color: rgba(148, 163, 184, 0.35);
}

.comment-head {
  display: flex;
  gap: 0.75rem;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 0.4rem;
}

.comment-author {
  font-size: 0.9rem;
  color: #0f172a;
}

.comment-time {
  color: #94a3b8;
  font-size: 0.8rem;
  white-space: nowrap;
}

.comment-body {
  margin: 0 0 0.5rem;
  white-space: pre-wrap;
  color: #0f172a;
  font-size: 0.925rem;
  line-height: 1.55;
}

.comment-node.is-deleted .comment-body {
  color: #94a3b8;
}

.comment-actions {
  display: flex;
  gap: 0.75rem;
  align-items: center;
}

.action-btn {
  border: 0;
  padding: 0;
  background: transparent;
  cursor: pointer;
  font-size: 0.825rem;
  font-weight: 600;
  line-height: 1;
}

.reply-btn,
.load-more-btn {
  color: #0f766e;
}

.reply-btn:hover,
.load-more-btn:hover {
  text-decoration: underline;
}

.delete-btn {
  color: #dc2626;
}

.delete-btn:hover {
  text-decoration: underline;
}

.cancel-btn {
  color: #64748b;
}

.cancel-btn:hover {
  text-decoration: underline;
}

/* Inline reply composer */
.inline-composer {
  margin-top: 0.65rem;
  padding: 0.75rem;
  border-radius: 0.75rem;
  border: 1px solid rgba(15, 118, 110, 0.25);
  background: #f0fdfa;
}

.reply-textarea {
  width: 100%;
  resize: vertical;
  padding: 0.65rem 0.75rem;
  border-radius: 0.65rem;
  border: 1px solid rgba(148, 163, 184, 0.4);
  font: inherit;
  font-size: 0.9rem;
  background: white;
  transition:
    border-color 0.15s,
    box-shadow 0.15s;
}

.reply-textarea:focus {
  outline: none;
  border-color: #0f766e;
  box-shadow: 0 0 0 3px rgba(15, 118, 110, 0.1);
}

.inline-composer-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 0.5rem;
  gap: 0.5rem;
}

.inline-composer-actions {
  display: flex;
  align-items: center;
  gap: 0.65rem;
}

.char-count {
  font-size: 0.78rem;
  color: #94a3b8;
}

.char-count.near-limit {
  color: #f59e0b;
}

.reply-submit-btn {
  padding: 0.4rem 1rem;
  border: 0;
  border-radius: 999px;
  background: linear-gradient(135deg, #0f766e, #0d9488);
  color: white;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.15s;
}

.reply-submit-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.children {
  margin-top: 0.5rem;
  padding-left: 0.75rem;
}

.load-more-btn {
  margin-top: 0.5rem;
  display: block;
}
</style>
