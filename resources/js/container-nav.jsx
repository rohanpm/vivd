import React    from 'react';

import Dispatch     from './dispatch';
import * as JsonApi from './json-api';

export default React.createClass({
  links: function() {
    return this.props.containers.links;
  },

  page: function(key) {
    const link = this.links()[key];
    if (!link) {
      return;
    }
    const url = JsonApi.linkUrl(link);

    JsonApi.xhr(
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
        <li key="prev" className="previous">
          <a href="#" onClick={this.pager('prev')}>
            <span aria-hidden="true">&larr;</span>
            Previous
          </a>
        </li>
      );
    }

    if (links.next) {
      pagers.push(
        <li key="next" className="next">
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
