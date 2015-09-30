import Dispatch   from '../dispatch';
import * as Links from '../links';

export default class {
  constructor(app) {
    this.app = app;

    Dispatch.on('request-log',         this.requestLog.bind(this));
    Dispatch.on('close-log',           this.closeLog.bind(this));
    Dispatch.on('show-log-timestamps', this.showLogTimestamps.bind(this));
  }

  setShowingLog(id) {
    const newUrl = Links.urlWithParams(this.app.state.currentUrl, {log: id});
    this.app.setState({showingLog: id},
                      () => Dispatch('url-activated', newUrl));
  }

  requestLog(c) {
    this.setShowingLog(c.id);
  }

  closeLog(c) {
    this.setShowingLog(null);
  }

  showLogTimestamps(showingLogTimestamps) {
    const newUrl = Links.urlWithParams(this.app.state.currentUrl, {logTimestamp: showingLogTimestamps ? 1 : null});
    this.app.setState({showingLogTimestamps},
                      () => Dispatch('url-activated', newUrl));
  }
}
