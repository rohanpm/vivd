import React    from 'react';
import $        from 'jquery';

import Body     from './body';
import Dispatch from './dispatch';

const App = React.createClass({
  getInitialState: function() {
    return this.props.initialState || {
      title: "vivd",
      containers: {data: []}
    };
  },
/*
  componentDidMount: function() {
    $.ajax("/a/containers").done(data => {
      console.log("got ", data);
      Dispatch('containers-queried', data);
    });
  },
*/
  componentWillMount: function() {
    Dispatch.on('containers-queried', data => {
      const newState = Object.assign({}, this.state);
      newState.containers = data;
      console.log("received: ", data);
      this.setState(newState);
    });
  },

  render: function() {
    return <Body {...this.state}/>;
  }
});

// bootstrap if loaded in browser
/*
if (document) {
  React.render(
    <App/>,
    document.getElementsByTagName('body')[0]
  );
}
*/

// This is used for rendering the application, server-side.
// Exporting it in this way because the server doesn't have a working
// module importer.
window.renderAppForState = function(state) {
  const app = <App initialState={state}/>;
  return React.renderToString(app);
};

export default App;
