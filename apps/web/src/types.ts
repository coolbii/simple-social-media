export type AuthStatus = 'checking' | 'authenticated' | 'anonymous';

export interface ApiEnvelope<T> {
  data: T;
}

export interface UserSummary {
  id: number;
  userName: string;
  phoneNumber: string;
  email?: string | null;
  coverImageUrl?: string | null;
  biography?: string | null;
}

export interface LoginResponse {
  user: UserSummary;
}

export interface PostItem {
  id: number;
  userId: number;
  userName: string;
  content: string;
  imageUrl?: string | null;
  createdAt: string;
  updatedAt?: string | null;
}

export interface CommentItem {
  id: number;
  postId: number;
  userId: number;
  userName: string;
  parentCommentId?: number | null;
  content: string;
  createdAt: string;
  deleted?: boolean;
}

export interface CommentCreatedEvent {
  type: 'comment.created';
  postId: number;
  comment: CommentItem;
}

export interface CommentPage {
  comments: CommentItem[];
  hasMore: boolean;
  nextOffset: number | null;
}
