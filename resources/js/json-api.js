export function doXhr({method, url, data, responseType, headers, onabort, onerror, onload,
                       onloadstart, onprogress, ontimeout, onloadend})
{
  const req = new XMLHttpRequest();
  headers = headers || {};

  req.responseType = responseType || 'json';
  req.onabort = onabort;
  req.onerror = onerror;
  req.onload = onload;
  req.onloadstart = onloadstart;
  req.onprogress = onprogress;
  req.ontimeout = ontimeout;
  req.onloadend = onloadend;

  req.open(method || 'GET', url, true);

  for (let key of Object.keys(headers)) {
    req.setRequestHeader(key, headers[key]);
  }

  if (data) {
    req.send(data);
  } else {
    req.send();
  }

  return req;
}

export function xhr(args) {
  args.headers = args.headers || {};
  args.headers['Accept'] = 'application/vnd.api+json';
  return doXhr(args);
}

export function linkUrl(link) {
  if (link.hasOwnProperty('href')) {
    return link['href'];
  }
  return link;
}
