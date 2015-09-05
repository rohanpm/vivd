import React       from 'react';
import QueryString from 'query-string';

import Body         from './body';
import Dispatch     from './dispatch';
import * as JsonApi from './json-api';
import debounce     from './debounce';

function updatedQueryString(params) {
  const now = QueryString.parse(location.search);
  const updated = Object.assign({}, now);
  for (let key of Object.keys(params)) {
    if (params[key] === null) {
      delete updated[key];
    } else {
      updated[key] = params[key];
    }
  }
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
    Dispatch("replace-api-params", {params:
                                    {"filter[*]":    str,
                                     "page[offset]": 0},
                                    then: () => this.setState({appliedFilter: str})});
  }, 300),

  setupEventSource: function() {
    var url;
    try {
      url = JsonApi.linkUrl(this.state.containers.links.events);
    } catch (e) {}
    if (!url) {
      console.warn("Missing 'events' link");
      return;
    }

    const source = new EventSource(url);
    source.onmessage = e => Dispatch('sse', JSON.parse(e.data));
  },

  mergeContainer: function(c) {
    const id = c.id;
    const data = this.state.containers.data;

    var anyUpdated = false;
    for (let stateC of data) {
      if (stateC.id === id) {
        anyUpdated = true;
        Object.assign(stateC, c);
      }
    }

    if (anyUpdated) {
      this.forceUpdate();
    }
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

    Dispatch.on('replace-api-params', ({params, then}) => {
      const search = location.search;
      const updatedSearch = updatedQueryString(params);
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
      this.setState(state, () => {
        history.pushState(this.state, "", search);
      });
    });

    Dispatch.on('sse', o => {
      if (o.type === "containers") {
        this.mergeContainer(o);
      }
    });

    Dispatch.on('request-log', c => {
      this.setState({showingLog: c.id}, () => {
        history.pushState(this.state, "", updatedQueryString({log: c.id}));
      });
    });
    Dispatch.on('close-log', () => {
      this.setState({showingLog: null}, () => {
        history.pushState(this.state, "", updatedQueryString({log: null}));
      });
    });
    Dispatch.on('show-log-timestamps', b => {
      this.setState({showingLogTimestamps: b}, () => {
        history.replaceState(this.state, "", updatedQueryString({logTimestamp: b ? 1 : null}));
      });
    });

    this.addHistoryHooks();
    this.setupEventSource();
  },

  render: function() {
    return <Body {...this.state}/>;
  }
});
