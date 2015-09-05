import * as JsonApi from '../json-api';

export default class {
  constructor(app) {
    this.app = app;

    var url;
    try {
      url = JsonApi.linkUrl(this.app.state.containers.links.events);
    } catch (e) {}

    if (!url) {
      console.warn("Missing 'events' link");
      return;
    }

    const source = new EventSource(url);
    source.onmessage = this.onMessage.bind(this);
  }

  onMessage(event) {
    const object = JSON.parse(event.data);
    if (object.type === "containers") {
      this.mergeContainer(object);
    }
  }

  mergeContainer(c) {
    const id = c.id;
    const data = this.app.state.containers.data;

    var anyUpdated = false;
    for (let stateC of data) {
      if (stateC.id === id) {
        anyUpdated = true;
        Object.assign(stateC, c);
      }
    }

    if (anyUpdated) {
      this.app.forceUpdate();
    }
  }
}
