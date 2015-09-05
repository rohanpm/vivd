import QueryString from 'query-string';

export function urlWithParams(url, params) {
  const split = url.split('?');
  const base = split[0];
  const search = split[1] || '';

  const now = QueryString.parse(search);
  const updated = Object.assign({}, now);
  for (let key of Object.keys(params)) {
    if (params[key] === null) {
      delete updated[key];
    } else {
      updated[key] = params[key];
    }
  }

  return `${base}?${QueryString.stringify(updated)}`;
}

export function currentUrlWithParams(params) {
  return urlWithParams(location.href, params);
}

export function adjustUrlForLink(link) {
  const meta = link.meta;
  if (!meta) {
    return null;
  }

  const query_params = meta['query-params'];
  if (!query_params) {
    return null;
  }

  return currentUrlWithParams(query_params);
}
