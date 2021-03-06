import React      from 'react';

import Dispatch   from './dispatch';
import GlyphIcon  from './glyph-icon';
import * as Links from './links';

export default React.createClass({
  getInitialState: function() {
    return {timestampedLogText: '',
            notTimestampedLogText: ''};
  },

  getDefaultProps: function() {
    return {showTimestamp: false};
  },

  toggleTimestamps: function() {
    Dispatch('show-log-timestamps', !this.props.showTimestamp);
  },

  getLogText: function() {
    return this.props.showTimestamp ? this.state.timestampedLogText : this.state.notTimestampedLogText;
  },

  getLogElem: function() {
    const text = this.getLogText();
    if (text == '') {
      return (
        <em>
          (Waiting for logs...)
        </em>
      );
    } else {
      return (
        <pre>
          {text}
        </pre>
      );
    }
  },

  addLogText: function(msg) {
    const firstSpace = msg.indexOf(' ');
    const msgWithoutTimestamp = msg.substring(firstSpace + 1);
    const timestampedLogText    = this.state.timestampedLogText + msg + "\n";
    const notTimestampedLogText = this.state.notTimestampedLogText + msgWithoutTimestamp + "\n";
    this.setState({timestampedLogText, notTimestampedLogText});
  },

  close: function(evt) {
    evt.preventDefault();
    Dispatch('close-log');
  },

  render: function() {
    return (
      <div className="panel panel-default" style={{marginTop: 20}}>
        <div className="panel-heading">
          <div className="row">
            <div className="col-md-3">
              <strong style={{fontSize: 18}}>
                Logs for {this.props.containerId}
              </strong>
            </div>
            <div className="col-md-2">
              <label>
                <input type="checkbox" onChange={this.toggleTimestamps} checked={this.props.showTimestamp}/>
                &nbsp;
                Show timestamps
              </label>
            </div>
            <div className="col-md-6"/>
            <div className="col-md-1">
              <a href={Links.urlWithParams(this.props.currentUrl, {log: null})} className="pull-right" onClick={this.close}>
                <GlyphIcon icon-type="remove"/>
              </a>
            </div>
          </div>
        </div>
        <div className="panel-body">
          {this.getLogElem()}
        </div>
      </div>
    );
  },

  componentDidMount: function() {
    this.eventSource = new EventSource(`/a/containers/${this.props.containerId}/logs`);
    this.eventSource.onmessage = message => {
      this.addLogText(message.data);
    };
  },

  componentWillUnmount: function() {
    this.eventSource.close();
  },
});
