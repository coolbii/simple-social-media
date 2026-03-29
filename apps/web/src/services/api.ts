import {
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
  type UserSummary as ApiUserSummary,
} from '@simple-social-media/api-contract';
import { resolveApiBaseUrl } from '@simple-social-media/utils';
import type { CommentItem, LoginResponse, PostItem, UserSummary } from '../types';

OpenAPI.BASE = resolveApiBaseUrl();
OpenAPI.WITH_CREDENTIALS = true;
OpenAPI.CREDENTIALS = 'include';

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
    updatedAt: required(post.updatedAt, 'post.updatedAt'),
  };
}

function toCommentItem(comment: CommentResponse): CommentItem {
  return {
    id: required(comment.id, 'comment.id'),
    postId: required(comment.postId, 'comment.postId'),
    userId: required(comment.userId, 'comment.userId'),
    userName: required(comment.userName, 'comment.userName'),
    content: required(comment.content, 'comment.content'),
    createdAt: required(comment.createdAt, 'comment.createdAt'),
  };
}

async function withAuth<T>(request: () => Promise<{ data?: T }>): Promise<T> {
  return unwrap(await request());
}

export async function register(payload: RegisterRequest): Promise<RegisterResponse> {
  const response = await AuthService.authRegister({ requestBody: payload });
  const data = unwrap<ApiResponseRegisterResponse>(response);
  return {
    userId: required(data.userId, 'register.data.userId'),
  };
}

export async function login(payload: LoginRequest): Promise<LoginResponse> {
  const response = await AuthService.authLogin({ requestBody: payload });
  return toLoginResponse(unwrap<ApiResponseLoginResponse>(response));
}

export async function logout() {
  const response = await AuthService.authLogout();
  const data = unwrap<ApiResponseLogoutResponse>(response);
  return {
    success: required(data.success, 'logout.data.success'),
  };
}

export async function getCurrentUser(): Promise<UserSummary> {
  const response = await withAuth<MeResponse>(() => AuthService.authMe());
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

export async function createComment(postId: number, payload: CreateCommentRequest): Promise<CommentItem> {
  const response = await withAuth<CommentResponse>(() =>
    CommentsService.createComment({ postId, requestBody: payload })
  );
  return toCommentItem(response);
}
