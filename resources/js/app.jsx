import React       from 'react';
import QueryString from 'query-string';

import Body         from './body';
import Dispatch     from './dispatch';
import * as JsonApi from './json-api';
import debounce     from './debounce';

function updatedQueryString(key, val) {
  const now = QueryString.parse(location.search);
  const updated = Object.assign({}, now);
  updated[key] = val;
  return '?' + QueryString.stringify(updated);
}

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
  const updated = Object.assign({}, now);
  var any = false;
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

  applyFilter: debounce(function(str) {
    Dispatch("replace-api-param", {param: "filter[*]",
                                   value: str,
                                   then: () => this.setState({appliedFilter: str})});
  }, 300),

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

    Dispatch.on('replace-api-param', ({param, value, then}) => {
      const search = location.search;
      const updatedSearch = updatedQueryString(param, value);
      if (search === updatedSearch) {
        console.log("No change to search", search);
        return;
      }

      const url = '/a/containers' + updatedSearch;
      JsonApi.xhr(
        {url: url,
         onload: (event) => {
           if (then) {
             then();
           }
           Dispatch('set-state-and-history', {state: {containers: event.target.response},
                                              search: updatedSearch});
         }
        }
      );
    });

    Dispatch.on('filter-requested', (str) => {
      this.setState({inputFilter: str});
      this.applyFilter(str);
    });

    Dispatch.on('set-state-and-history', ({state, search}) => {
      this.setState(state);
      history.pushState(this.state, "", search);
    });

    this.addHistoryHooks();
  },

  render: function() {
    return <Body {...this.state}/>;
  }
});
