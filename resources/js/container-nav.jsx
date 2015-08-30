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

export default React.createClass({
  links: function() {
    return this.props.containers.links;
  },

  page: function(key) {
    const url = this.links()[key];
    if (!url) {
      return;
    }

    apiXhr(
      {url: url,
       onloadend: (event) => {
         Dispatch('ajax-finished');
         Dispatch('paged', event.target.response);
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
    
    return (
      <nav>
        <ul className="pager">
          <li className={'previous ' +  (links.prev ? '' : 'disabled')}>
            <a href="#" onClick={this.pager('prev')}>
              <span aria-hidden="true">&larr;</span>
              Previous
            </a>
          </li>
          <li className={'next ' + (links.next ? '' : 'disabled')}>
            <a href="#" onClick={this.pager('next')}>
              Next
              <span aria-hidden="true">&rarr;</span>
            </a>
          </li>
        </ul>
      </nav>
    );
  },
});
