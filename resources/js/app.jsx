import React       from 'react';
import QueryString from 'query-string';

import AppHistory   from './app/history';
import AppFilter    from './app/filter';
import Body         from './body';
import Dispatch     from './dispatch';
import * as JsonApi from './json-api';
import * as Links   from './links';

export default React.createClass({

  getInitialState: function() {
    return this.props.initialState || {
      title: "vivd",
      containers: {data: []}
    };
  },

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

    this.components = [
      new AppHistory(this),
      new AppFilter(this),
    ];

    this.setupEventSource();
  },

  render: function() {
    return <Body {...this.state}/>;
  }
});
