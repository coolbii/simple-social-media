function isLoopbackHost(hostname: string): boolean {
  return hostname === 'localhost' || hostname === '127.0.0.1' || hostname === '::1' || hostname === '[::1]';
}

export function resolveApiBaseUrl(): string {
  const configuredBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim() || '';
  if (!configuredBaseUrl || typeof window === 'undefined') {
    return configuredBaseUrl;
  }

  try {
    const apiUrl = new URL(configuredBaseUrl);
    if (isLoopbackHost(apiUrl.hostname) && isLoopbackHost(window.location.hostname)) {
      apiUrl.hostname = window.location.hostname;
      return apiUrl.toString().replace(/\/$/, '');
    }
  } catch {
    return configuredBaseUrl;
  }

  return configuredBaseUrl;
}

export function resolveSseUrl(path: string): string {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  const baseUrl = resolveApiBaseUrl();
  return baseUrl ? `${baseUrl}${normalizedPath}` : normalizedPath;
}
