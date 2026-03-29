import {
  ApiError,
  AuthService,
  CommentsService,
  OpenAPI,
  PostsService,
  type ApiResponseLoginResponse,
  type ApiResponseLogoutResponse,
  type ApiResponseRegisterResponse,
  type CommentResponse,
  type CreateCommentRequest,
  type CreatePostRequest,
  type DeletePostResponse,
  type LoginResponse as ApiLoginResponse,
  type LoginRequest,
  type MeResponse,
  type PostResponse,
  type RegisterRequest,
  type RegisterResponse,
  type SendCodeResponse,
  type UserSummary as ApiUserSummary,
  type VerifyCodeResponse,
} from '@simple-social-media/api-contract';
import { resolveApiBaseUrl } from '@simple-social-media/utils';
import type { CommentItem, LoginResponse, PostItem, UserSummary } from '../types';

OpenAPI.BASE = resolveApiBaseUrl();
OpenAPI.WITH_CREDENTIALS = true;
OpenAPI.CREDENTIALS = 'include';

interface ApiErrorBody {
  error?: {
    message?: string;
  };
}

function unwrap<T extends { data?: unknown }>(response: T): NonNullable<T['data']> {
  if (response.data === undefined || response.data === null) {
    throw new Error('OpenAPI response did not contain a data payload.');
  }

  return response.data as NonNullable<T['data']>;
}

function required<T>(value: T | null | undefined, field: string): NonNullable<T> {
  if (value === undefined || value === null) {
    throw new Error(`OpenAPI response missing required field: ${field}`);
  }

  return value as NonNullable<T>;
}

function toUserSummary(user: ApiUserSummary | MeResponse, fieldPrefix: string): UserSummary {
  return {
    id: required(user.id, `${fieldPrefix}.id`),
    userName: required(user.userName, `${fieldPrefix}.userName`),
    phoneNumber: required(user.phoneNumber, `${fieldPrefix}.phoneNumber`),
    email: 'email' in user ? (user.email ?? null) : null,
    coverImageUrl: 'coverImageUrl' in user ? (user.coverImageUrl ?? null) : null,
    biography: 'biography' in user ? (user.biography ?? null) : null,
  };
}

function toLoginResponse(response: ApiLoginResponse): LoginResponse {
  return {
    user: toUserSummary(required(response.user, 'login.data.user'), 'login.data.user'),
  };
}

function toPostItem(post: PostResponse): PostItem {
  return {
    id: required(post.id, 'post.id'),
    userId: required(post.userId, 'post.userId'),
    userName: required(post.userName, 'post.userName'),
    content: required(post.content, 'post.content'),
    imageUrl: post.imageUrl ?? null,
    createdAt: required(post.createdAt, 'post.createdAt'),
    updatedAt: post.updatedAt ?? null,
  };
}

function toCommentItem(comment: CommentResponse): CommentItem {
  const enrichedComment = comment as CommentResponse & {
    parentCommentId?: number | null;
    deleted?: boolean;
  };
  const parentCommentId = enrichedComment.parentCommentId ?? null;
  return {
    id: required(comment.id, 'comment.id'),
    postId: required(comment.postId, 'comment.postId'),
    userId: required(comment.userId, 'comment.userId'),
    userName: required(comment.userName, 'comment.userName'),
    parentCommentId,
    content: required(comment.content, 'comment.content'),
    createdAt: required(comment.createdAt, 'comment.createdAt'),
    deleted: enrichedComment.deleted ?? false,
  };
}

async function withAuth<T>(request: () => Promise<{ data?: T }>): Promise<T> {
  return unwrap(await request());
}

function rethrowApiError(error: unknown): never {
  if (error instanceof ApiError) {
    const serverMessage = (error.body as ApiErrorBody | undefined)?.error?.message;
    if (typeof serverMessage === 'string' && serverMessage.trim().length > 0) {
      throw new Error(serverMessage);
    }
  }
  if (error instanceof Error) {
    throw error;
  }
  throw new Error('Request failed.');
}

export async function register(payload: RegisterRequest): Promise<RegisterResponse> {
  try {
    const response = await AuthService.authRegister({ requestBody: payload });
    const data = unwrap<ApiResponseRegisterResponse>(response);
    return {
      userId: required(data.userId, 'register.data.userId'),
    };
  } catch (error) {
    rethrowApiError(error);
  }
}

export async function login(payload: LoginRequest): Promise<LoginResponse> {
  try {
    const response = await AuthService.authLogin({ requestBody: payload });
    return toLoginResponse(unwrap<ApiResponseLoginResponse>(response));
  } catch (error) {
    rethrowApiError(error);
  }
}

export async function logout() {
  const response = await AuthService.authLogout();
  const data = unwrap<ApiResponseLogoutResponse>(response);
  return {
    success: required(data.success, 'logout.data.success'),
  };
}

export async function sendCode(phoneNumber: string): Promise<SendCodeResponse> {
  try {
    const response = await AuthService.authSendCode({ requestBody: { phoneNumber } });
    return unwrap(response);
  } catch (error) {
    rethrowApiError(error);
  }
}

export async function verifyCode(phoneNumber: string, code: string): Promise<VerifyCodeResponse> {
  try {
    const response = await AuthService.authVerifyCode({ requestBody: { phoneNumber, code } });
    return unwrap(response);
  } catch (error) {
    rethrowApiError(error);
  }
}

export async function getCurrentUser(): Promise<UserSummary> {
  const response = await withAuth<MeResponse>(() => AuthService.authMe({}));
  return toUserSummary(response, 'me.data');
}

export async function fetchPosts(): Promise<PostItem[]> {
  const response = await withAuth<PostResponse[]>(() => PostsService.listPosts());
  return response.map(toPostItem);
}

export async function createPost(payload: CreatePostRequest): Promise<PostItem> {
  const response = await withAuth<PostResponse>(() => PostsService.createPost({ requestBody: payload }));
  return toPostItem(response);
}

export async function fetchPost(postId: number): Promise<PostItem> {
  const response = await withAuth<PostResponse>(() => PostsService.getPostDetail({ postId }));
  return toPostItem(response);
}

export function updatePost(postId: number, payload: { content: string; imageUrl?: string }): Promise<PostItem> {
  return withAuth<PostResponse>(() => PostsService.updatePost({ postId, requestBody: payload })).then(toPostItem);
}

export async function deletePost(postId: number): Promise<DeletePostResponse> {
  const response = await withAuth<DeletePostResponse>(() => PostsService.deletePost({ postId }));
  return {
    success: required(response.success, 'deletePost.data.success'),
  };
}

export async function fetchComments(postId: number): Promise<CommentItem[]> {
  const response = await withAuth<CommentResponse[]>(() => CommentsService.listCommentsByPost({ postId }));
  return response.map(toCommentItem);
}

export async function createComment(
  postId: number,
  payload: { content: string; parentCommentId?: number | null }
): Promise<CommentItem> {
  const response = await withAuth<CommentResponse>(() =>
    CommentsService.createComment({ postId, requestBody: payload as unknown as CreateCommentRequest })
  );
  return toCommentItem(response);
}

export async function deleteComment(postId: number, commentId: number): Promise<CommentItem> {
  const url = OpenAPI.BASE
    ? `${OpenAPI.BASE}/api/posts/${postId}/comments/${commentId}`
    : `/api/posts/${postId}/comments/${commentId}`;

  const response = await fetch(url, {
    method: 'DELETE',
    credentials: 'include',
  });

  const payload = await response.json().catch(() => null);
  if (!response.ok) {
    const message =
      typeof payload?.error?.message === 'string' && payload.error.message.trim().length > 0
        ? payload.error.message
        : 'Delete comment failed. Please try again.';
    throw new Error(message);
  }

  const data = payload?.data as CommentResponse | undefined;
  if (!data) {
    throw new Error('Delete comment response missing data.');
  }

  return toCommentItem(data);
}

export async function updateComment(
  postId: number,
  commentId: number,
  payload: { content: string }
): Promise<CommentItem> {
  const url = OpenAPI.BASE
    ? `${OpenAPI.BASE}/api/posts/${postId}/comments/${commentId}`
    : `/api/posts/${postId}/comments/${commentId}`;

  const response = await fetch(url, {
    method: 'PUT',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  });

  const body = await response.json().catch(() => null);
  if (!response.ok) {
    const message =
      typeof body?.error?.message === 'string' && body.error.message.trim().length > 0
        ? body.error.message
        : 'Update comment failed. Please try again.';
    throw new Error(message);
  }

  const data = body?.data as CommentResponse | undefined;
  if (!data) {
    throw new Error('Update comment response missing data.');
  }
  return toCommentItem(data);
}

export async function uploadPostImage(file: File): Promise<string> {
  const formData = new FormData();
  formData.append('file', file);

  const uploadUrl = OpenAPI.BASE ? `${OpenAPI.BASE}/api/uploads/post-image` : '/api/uploads/post-image';
  const response = await fetch(uploadUrl, {
    method: 'POST',
    credentials: 'include',
    body: formData,
  });

  const payload = await response.json().catch(() => null);
  if (!response.ok) {
    const message =
      typeof payload?.error?.message === 'string' && payload.error.message.trim().length > 0
        ? payload.error.message
        : 'Image upload failed. Please try again.';
    throw new Error(message);
  }

  const objectKey = payload?.data?.objectKey;
  if (typeof objectKey !== 'string' || objectKey.trim().length === 0) {
    throw new Error('Upload response missing object key.');
  }

  return objectKey;
}
