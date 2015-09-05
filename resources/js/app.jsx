import React          from 'react';

import AppHistory     from './app/history';
import AppFilter      from './app/filter';
import AppEventSource from './app/eventsource';
import AppPager       from './app/pager';
import AppLogs        from './app/logs';
import Body           from './body';

export default React.createClass({
  getInitialState: function() {
    return this.props.initialState || {
      title: "vivd",
      containers: {data: []}
    };
  },

  componentDidMount: function() {
    this.components = [
      new AppHistory(this),
      new AppFilter(this),
      new AppEventSource(this),
      new AppPager(this),
      new AppLogs(this),
    ];
  },

  render: function() {
    return <Body {...this.state}/>;
  }
});
