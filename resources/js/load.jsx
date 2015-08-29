import React from 'react';

import App   from './app';

const inBrowser = (typeof(document) !== 'undefined');

if (inBrowser) {
  // (re-)render whatever state was passed by the server
  React.render(
    <App initialState={serverState}/>,
    document.getElementsByTagName('body')[0]
  );
} else {
  // This is used for rendering the application, server-side.
  // Exporting it in this way because the server doesn't have a working
  // module importer.
  window.renderAppForState = function(state) {
    const app = <App initialState={state}/>;
    return React.renderToString(app);
  };
}
