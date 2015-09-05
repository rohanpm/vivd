import React from 'react';

import * as JsonApi from './json-api';
import GlyphIcon    from './glyph-icon';
import * as Links   from './links';
import Dispatch     from './dispatch';

function statusAttributes(status) {
  const statusMap = {
    up: {
      button_type: 'success',
      text:        'Running',
      icon_type:   'play',
    },

    'new': {
      button_type: 'info',
      text:        'New',
      icon_type:   'pause',
    },

    building: {
      button_type: 'warning',
      text:        'Building',
      icon_type:   'hourglass',
    },

    built: {
      button_type: 'warning',
      text:        'Built',
      icon_type:   'hourglass',
    },

    starting: {
      button_type: 'warning',
      text:        'Starting',
      icon_type:   'hourglass',
    },

    stopped: {
      button_type: 'info',
      text:        'Stopped',
      icon_type:   'stop',
    },

    'timed-out': {
      button_type: 'danger',
      text:        'Timed Out',
      icon_type:   'flag',
    },
  };

  statusMap.stopping = statusMap.stopped;

  const defaultAttrs = {
    button_type: 'default',
    text:        'Unknown',
    icon_type:   'question-sign',
  };

  return statusMap[status] || defaultAttrs;
}

export default React.createClass({
  getInitialState: function() {
    return {open: false};
  },

  uiAttrs: function() {
    return statusAttributes(this.props.container.attributes.status);
  },

  requestLogs: function(evt) {
    evt.preventDefault();
    Dispatch('request-log', this.props.container);
  },

  menuItems: function() {
    const out = [];
    const links = this.props.container.links;

    if (links.logs) {
      out.push(
        <li key="logs">
          <a href={Links.currentUrlWithParams({log: this.props.container.id})}
            onClick={this.requestLogs}>
            Logs
          </a>
        </li>
      );
    }

    return out;
  },

  toggleOpen: function() {
    this.setState({open: !this.state.open});
  },

  render: function() {
    const attr = this.uiAttrs();
    const btnClass = `btn btn-${attr.button_type}`;
    const topClass = "btn-group " + (this.state.open ? 'open' : '');

    return (
      <div className={topClass}>
        <button type="button" className={btnClass}>
          <span className="pull-left">
            &nbsp;
            <GlyphIcon icon-type={attr.icon_type} aria-hidden="true"/>
            &nbsp;
          </span>
          <span className="hidden-xs">
            {attr.text}
          </span>
        </button>
        <button type="button" className={btnClass + ' dropdown-toggle'} onClick={this.toggleOpen} data-toggle="dropdown">
          <span className="caret"></span>
        </button>
        <ul className="dropdown-menu">
          {this.menuItems()}
        </ul>
      </div>
    );
  }
});
