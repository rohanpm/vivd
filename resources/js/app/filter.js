import Dispatch     from '../dispatch';
import debounce     from '../debounce';
import * as JsonApi from '../json-api';
import * as Links   from '../links';

export default class {
  constructor(app) {
    this.app = app;

    this.debouncedApplyFilter = debounce(this.applyFilter, 300);

    Dispatch.on('filter-requested', this.filterRequested.bind(this));
  }

  filterRequested(str) {
    this.app.setState({inputFilter: str});
    if (str == '') {
      this.applyFilter(str);
    } else {
      this.debouncedApplyFilter(str);
    }
  }

  applyFilter(str) {
    const params = {"filter[*]":    (str === '') ? null : str,
                    "page[offset]": null};

    // NOTE: requires index and API to have compatible params
    const apiUrl = Links.urlWithParams('/a/containers' + location.search, params);
    const uiUrl  = Links.urlWithParams(this.app.state.currentUrl, params);
    const onload = (event) => {
      this.app.setState(
        {appliedFilter: str,
         containers: event.target.response},
        () => Dispatch('url-activated', uiUrl)
      );
    };

    JsonApi.xhr({url: apiUrl, onload});
  }
}
