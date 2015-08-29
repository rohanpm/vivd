import React from 'react';

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
  uiAttrs: function() {
    return statusAttributes(this.props.container.attributes.status);
  },

  render: function() {
    const attr = this.uiAttrs();
    const btnClass = `btn btn-block btn-${attr.button_type}`;
    const iconClass = `glyphicon glyphicon-${attr.icon_type}`;

    return (
      <button type="button" className={btnClass}>
        <span className="pull-left">
          &nbsp;
          <span className={iconClass} aria-hidden="true"/>
          &nbsp;
        </span>
        <span className="hidden-xs">
          {attr.text}
        </span>
      </button>
    );
  }
});
