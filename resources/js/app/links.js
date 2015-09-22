import Dispatch     from '../dispatch';
import * as JsonApi from '../json-api';

export default class {
  constructor(app) {
    this.app = app;

    Dispatch.on('post-link', this.postLink.bind(this));
  }

  postLink(link) {
    const url = JsonApi.linkUrl(link);
    JsonApi.xhr({method: 'POST', url})
  }
}
