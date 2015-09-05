import QueryString from 'query-string';

export function currentUrlWithParams(params) {
  const hereWithoutSearch = location ? `${location.protocol}//${location.host}${location.pathname}` : '/';
  const now = QueryString.parse(location ? location.search : '');
  const updated = Object.assign({}, now);
  for (let key of Object.keys(params)) {
    if (params[key] === null) {
      delete updated[key];
    } else {
      updated[key] = params[key];
    }
  }
  return `${hereWithoutSearch}?${QueryString.stringify(updated)}`;
}
