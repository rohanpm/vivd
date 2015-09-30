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
    Dispatch.on('url-activated',  (url)  => this.urlActivated(url));
  }

  linkActivated(link) {
    // When a link is activated, if the link has meta for updating the query
    // string, then push it along with current state.
    const newUrl = Links.adjustUrlForLink(this.app.state.currentUrl, link);
    if (newUrl) {
      this.urlActivated(newUrl);
    }
  }

  urlActivated(url) {
    this.app.setState(
      {currentUrl: url},
      () => history.pushState(this.app.state, "", url)
    );
  }

}
