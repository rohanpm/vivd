import React    from 'react';

import Dispatch        from './dispatch';
import * as Links      from './links';
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
    Dispatch('page-requested', link);
  },

  pager: function(key) {
    return event => {
      event.preventDefault();
      return this.page(key);
    };
  },

  pagerButton: function({name, text, arrow}) {
    const links = this.links();
    const link = links[name];
    if (link) {
      return (
        <a className="btn btn-default" href={Links.adjustUrlForLink(this.props.currentUrl, link)}
           onClick={this.pager(name)}>
          <span aria-hidden="true">{arrow}</span>
          {text}
        </a>
      );
    }
  },

  render: function() {
    var prevElem = this.pagerButton({name: 'prev', text: 'Previous', arrow: '←'});
    var nextElem = this.pagerButton({name: 'next', text: 'Next',     arrow: '→'});
    
    return (
      <div className="row container-nav">
        <div className="col-md-2 pull-left">
          {prevElem}
        </div>
        <div className="col-md-1"/>
        <div className="col-md-6">
          <ContainerSearch currentUrl={this.props.currentUrl} filter={this.props.filter}/>
        </div>
        <div className="col-md-1"/>
        <div className="col-md-2 pull-right">
          {nextElem}
        </div>
      </div>
    );
  },
});
