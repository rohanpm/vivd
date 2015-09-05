import React      from 'react';

import Dispatch   from './dispatch';
import GlyphIcon  from './glyph-icon';
import * as Links from './links';

export default React.createClass({
  getInitialState: function() {
    return {logText: ''};
  },

  close: function(evt) {
    evt.preventDefault();
    Dispatch('close-log');
  },

  render: function() {
    return (
      <div className="panel panel-default" style={{marginTop: 20}}>
        <div className="panel-heading">
          <h3 className="panel-title">
            Logs for {this.props.containerId}
            <a href={Links.currentUrlWithParams({log: null})} className="pull-right" onClick={this.close}>
              <GlyphIcon icon-type="remove"/>
            </a>
          </h3>
        </div>
        <div className="panel-body">
          <pre>
            {this.state.logText}
          </pre>
        </div>
      </div>
    );
  },

  componentDidMount: function() {
    this.eventSource = new EventSource(`/a/containers/${this.props.containerId}/logs`);
    this.eventSource.onmessage = message => {
      this.setState({logText: this.state.logText + message.data + "\n"});
    };
  },

  componentWillUnmount: function() {
    this.eventSource.close();
  },
});
