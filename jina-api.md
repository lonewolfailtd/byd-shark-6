# Jina AI API Reference

## Authentication
Bearer Token: `jina_f9a67950836f467f9bf2d76236e9f587jreZ1638UBCZUk8t4oEbn349idQt`

## Endpoints

### 1. URL Fetch (Reader API)
Fetches and converts web pages to markdown.

```bash
curl "https://r.jina.ai/https://www.example.com" \
  -H "Authorization: Bearer jina_f9a67950836f467f9bf2d76236e9f587jreZ1638UBCZUk8t4oEbn349idQt"
```

### 2. Web Search (SERP API)
Searches the web and returns results.

```bash
curl "https://s.jina.ai/?q=your+search+query" \
  -H "Authorization: Bearer jina_f9a67950836f467f9bf2d76236e9f587jreZ1638UBCZUk8t4oEbn349idQt"
```

#### Optional Headers for Search
- `X-Respond-With: no-content` - Returns metadata only without fetching content

## Usage Examples

### Search for BYD information
```bash
curl "https://s.jina.ai/?q=BYD+Shark+6+head+unit+modifications" \
  -H "Authorization: Bearer jina_f9a67950836f467f9bf2d76236e9f587jreZ1638UBCZUk8t4oEbn349idQt"
```

### Fetch a specific webpage
```bash
curl "https://r.jina.ai/https://xdaforums.com/t/byd-multimedia-install-apk.4541247/" \
  -H "Authorization: Bearer jina_f9a67950836f467f9bf2d76236e9f587jreZ1638UBCZUk8t4oEbn349idQt"
```
