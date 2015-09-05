import Dispatch     from '../dispatch';
import * as JsonApi from '../json-api';

export default class {
  constructor(app) {
    this.app = app;

    Dispatch.on('page-requested', this.pageRequested.bind(this));
  }

  pageRequested(link) {
    const url    = JsonApi.linkUrl(link);
    const onload = (event) => {
      this.app.setState({containers: event.target.response});
      Dispatch('link-activated', link);
    };

    JsonApi.xhr({url, onload});
  }
}
