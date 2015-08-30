import React    from 'react';

import Body     from './body';
import Dispatch from './dispatch';

export default React.createClass({
  getInitialState: function() {
    return this.props.initialState || {
      title: "vivd",
      containers: {data: []}
    };
  },

  componentWillMount: function() {
    Dispatch.on('ajax-started', () => {
      this.setState({loading: true});
    });

    Dispatch.on('ajax-finished', () => {
      this.setState({loading: false});
    });

    Dispatch.on('paged', (obj) => {
      this.setState({containers: obj});
    });
  },

  render: function() {
    return <Body {...this.state}/>;
  }
});
