import React    from 'react';

import Dispatch from './dispatch';

function xhr({method, url, data, responseType, headers, onabort, onerror, onload,
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

function apiXhr(args) {
  args.headers = args.headers || {};
  args.headers['Accept'] = 'application/vnd.api+json';
  return xhr(args);
}

function linkUrl(link) {
  if (link.hasOwnProperty('href')) {
    return link['href'];
  }
  return link;
}

export default React.createClass({
  links: function() {
    return this.props.containers.links;
  },

  page: function(key) {
    const link = this.links()[key];
    if (!link) {
      return;
    }
    const url = linkUrl(link);

    apiXhr(
      {url: url,
       onloadend: (event) => {
         Dispatch('ajax-finished');
         Dispatch('paged', event.target.response);
         Dispatch('link-activated', link);
       }
      }
    );
    Dispatch('ajax-started');
  },

  pager: function(key) {
    return event => {
      event.preventDefault();
      return this.page(key);
    };
  },

  render: function() {
    const links = this.links();
    const pagers = [];

    if (links.prev) {
      pagers.push(
        <li className="previous">
          <a href="#" onClick={this.pager('prev')}>
            <span aria-hidden="true">&larr;</span>
            Previous
          </a>
        </li>
      );
    }

    if (links.next) {
      pagers.push(
        <li className="next">
          <a href="#" onClick={this.pager('next')}>
            Next
            <span aria-hidden="true">&rarr;</span>
          </a>
        </li>
      );
    }
    
    return (
      <nav>
        <ul className="pager">
          {pagers}
        </ul>
      </nav>
    );
  },
});
