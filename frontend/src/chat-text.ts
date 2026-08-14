export function cleanAssistantText(text: string): string {
  return text
    .replace(/\r\n?/g, '\n')
    .replace(/!\[([^\]]*)\]\([^)]*\)/g, '$1')
    .replace(/\[([^\]]+)\]\([^)]*\)/g, '$1')
    .replace(/^\s{0,3}#{1,6}\s*/gm, '')
    .replace(/^\s*(?:[-*_]\s*){3,}\s*$/gm, '')
    .replace(/^\s*[-+*]\s+/gm, '• ')
    .replace(/^\s*>\s?/gm, '')
    .replace(/[\-#__*`|]/g, '')
    .replace(/[ \t]+\n/g, '\n')
    .replace(/\n{3,}/g, '\n\n')
}
