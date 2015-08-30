import React       from 'react';
import QueryString from 'query-string';

import Body     from './body';
import Dispatch from './dispatch';

function searchForLink(link) {
  const meta = link.meta;
  if (!meta) {
    return null;
  }

  const query_params = meta['query-params'];
  if (!query_params) {
    return null;
  }

  const now = QueryString.parse(location.search);
  const updated = {};
  var any = false;
  for (let key of Object.keys(now)) {
    updated[key] = now[key];
  }
  for (let key of Object.keys(query_params)) {
    any = true;
    updated[key] = query_params[key];
  }

  if (!any) {
    return null;
  }

  return QueryString.stringify(updated);
}

export default React.createClass({
  addHistoryHooks: function() {
    Dispatch.on('link-activated', (link) => {
      // When a link is activated, if the link has meta for updating the query
      // string, then push it along with current state.
      const newSearch = searchForLink(link);
      if (newSearch) {
        history.pushState(this.state, "", '?' + newSearch);
      }
    });

    window.onpopstate = event => {
      this.setState(event.state);
    };

    // need to associate the initial state
    history.replaceState(this.state, "", "");
  },

  getInitialState: function() {
    return this.props.initialState || {
      title: "vivd",
      containers: {data: []}
    };
  },

  componentDidMount: function() {
    Dispatch.on('ajax-started', () => {
      this.setState({loading: true});
    });

    Dispatch.on('ajax-finished', () => {
      this.setState({loading: false});
    });

    Dispatch.on('paged', (obj) => {
      this.setState({containers: obj});
    });

    this.addHistoryHooks();
  },

  render: function() {
    return <Body {...this.state}/>;
  }
});
