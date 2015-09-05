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
    this.app.setState({showingLog: id},
                      () => history.pushState(
                        this.app.state,
                        '',
                        Links.currentUrlWithParams({log: id})));
  }

  requestLog(c) {
    this.setShowingLog(c.id);
  }

  closeLog(c) {
    this.setShowingLog(null);
  }

  showLogTimestamps(showingLogTimestamps) {
    this.app.setState({showingLogTimestamps},
                      () => history.replaceState(
                        this.app.state,
                        '',
                        Links.currentUrlWithParams({logTimestamp: showingLogTimestamps ? 1 : null})));
  }
}
