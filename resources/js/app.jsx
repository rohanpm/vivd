import React       from 'react';
import QueryString from 'query-string';

import Body         from './body';
import Dispatch     from './dispatch';
import * as JsonApi from './json-api';
import debounce     from './debounce';
import * as Links   from './links';

export default React.createClass({
  addHistoryHooks: function() {
    Dispatch.on('link-activated', (link) => {
      // When a link is activated, if the link has meta for updating the query
      // string, then push it along with current state.
      const newUrl = Links.adjustUrlForLink(link);
      if (newUrl) {
        history.pushState(this.state, "", newUrl);
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
                                    {"filter[*]":    (str === '') ? null : str,
                                     "page[offset]": null},
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
      // NOTE: requires index and API to have compatible params
      const apiUrl = Links.urlWithParams('/a/containers' + location.search, params);
      const uiUrl = Links.currentUrlWithParams(params);
      JsonApi.xhr(
        {url: apiUrl,
         onload: (event) => {
           if (then) {
             then();
           }
           this.setState({containers: event.target.response}, () => {
             history.pushState(this.state, "", uiUrl);
           });
         }
        }
      );
    });

    Dispatch.on('filter-requested', (str) => {
      this.setState({inputFilter: str});
      this.applyFilter(str);
    });

    Dispatch.on('sse', o => {
      if (o.type === "containers") {
        this.mergeContainer(o);
      }
    });

    Dispatch.on('request-log', c => {
      this.setState({showingLog: c.id}, () => {
        history.pushState(this.state, "", Links.currentUrlWithParams({log: c.id}));
      });
    });
    Dispatch.on('close-log', () => {
      this.setState({showingLog: null}, () => {
        history.pushState(this.state, "", Links.currentUrlWithParams({log: null}));
      });
    });
    Dispatch.on('show-log-timestamps', b => {
      this.setState({showingLogTimestamps: b}, () => {
        history.replaceState(this.state, "", Links.currentUrlWithParams({logTimestamp: b ? 1 : null}));
      });
    });

    this.addHistoryHooks();
    this.setupEventSource();
  },

  render: function() {
    return <Body {...this.state}/>;
  }
});
