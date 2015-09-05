import React    from 'react';

import Dispatch        from './dispatch';
import * as JsonApi    from './json-api';
import ContainerSearch from './container-search';

export default React.createClass({
  getDefaultProps: function() {
    return {containers: {links: {}}};
  },

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
       onload: (event) => {
         Dispatch('paged', event.target.response);
         Dispatch('link-activated', link);
       }
      }
    );
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

    var prevElem = null;
    var nextElem = null;

    if (links.prev) {
      prevElem = (
        <a className="btn btn-default" href="#" onClick={this.pager('prev')}>
          <span aria-hidden="true">&larr;</span>
          Previous
        </a>
      );
    }

    if (links.next) {
      nextElem = (
        <a className="btn btn-default" href="#" onClick={this.pager('next')}>
          Next
          <span aria-hidden="true">&rarr;</span>
        </a>
      );
    }
    
    return (
      <div className="row container-nav">
        <div className="col-md-2 pull-left">
          {prevElem}
        </div>
        <div className="col-md-1"/>
        <div className="col-md-6">
          <ContainerSearch filter={this.props.filter}/>
        </div>
        <div className="col-md-1"/>
        <div className="col-md-2 pull-right">
          {nextElem}
        </div>
      </div>
    );
  },
});
