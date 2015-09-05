import Dispatch   from '../dispatch';
import * as Links from '../links';

export default class {
  constructor(app) {
    this.app = app;

    window.onpopstate = event => {
      app.setState(event.state);
    };

    // need to associate the initial state
    history.replaceState(app.state, "", "");

    Dispatch.on('link-activated', (link) => this.linkActivated(link));
  }

  linkActivated(link) {
    // When a link is activated, if the link has meta for updating the query
    // string, then push it along with current state.
    const newUrl = Links.adjustUrlForLink(link);
    if (newUrl) {
      history.pushState(this.app.state, "", newUrl);
    }
  }
}
