import React       from 'react';
import QueryString from 'query-string';

import AppHistory     from './app/history';
import AppFilter      from './app/filter';
import AppEventSource from './app/eventsource';
import Body           from './body';
import Dispatch       from './dispatch';
import * as JsonApi   from './json-api';
import * as Links     from './links';

export default React.createClass({

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
      new AppEventSource(this),
    ];
  },

  render: function() {
    return <Body {...this.state}/>;
  }
});
