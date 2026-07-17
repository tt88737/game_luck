export const resolveResponseMessage = (code: number | string, backendMessage?: string, fallbackMessage?: string) => {
  void code;
  return backendMessage || fallbackMessage || '';
};
