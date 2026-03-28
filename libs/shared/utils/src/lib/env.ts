export function resolveApiBaseUrl(): string {
  return import.meta.env.VITE_API_BASE_URL?.trim() || '';
}

export function resolveSseUrl(path: string): string {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  const baseUrl = resolveApiBaseUrl();
  return baseUrl ? `${baseUrl}${normalizedPath}` : normalizedPath;
}
