// This file is loaded on the server (only) prior to app bundle.

// Define this global so it can be used to stash things from within the
// bundle.
window = {};

// This is filled in elsewhere with info from the request
location = {};

console = {
  _jlog: java.util.logging.Logger.getLogger('app-bundle.js'),
  log: function() {
    console._jlog.fine(Array.prototype.slice.call(arguments).toString());
  },
  warn: function() {
    console._jlog.warning(Array.prototype.slice.call(arguments).toString());
  },
  error: function() {
    console._jlog.severe(Array.prototype.slice.call(arguments).toString());
  },
};
