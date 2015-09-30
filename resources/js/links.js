import QueryString from 'query-string';

// These parameters can be deleted if set to these values.
const deleteParams = {
  'page[offset]': 0,
};

export function urlWithParams(url, params) {
  const split = url.split('?');
  const base = split[0];
  const search = split[1] || '';

  const now = QueryString.parse(search);
  const updated = Object.assign({}, now);
  for (let key of Object.keys(params)) {
    const val = params[key];
    if (val === null || (deleteParams.hasOwnProperty(key) && deleteParams[key] == val)) {
      delete updated[key];
    } else {
      updated[key] = params[key];
    }
  }

  return `${base}?${QueryString.stringify(updated)}`;
}

export function adjustUrlForLink(url, link) {
  const meta = link.meta;
  if (!meta) {
    return null;
  }

  const query_params = meta['query-params'];
  if (!query_params) {
    return null;
  }

  return urlWithParams(url, query_params);
}
