import React          from 'react';

import ContainerLogs  from './container-logs';
import ContainerTable from './container-table';
import ContainerNav   from './container-nav';

export default React.createClass({
  render: function() {
    if (this.props.showingLog) {
      return this.renderShowingLog();
    }
    return this.renderContainerTable();
  },

  renderShowingLog: function() {
    return (
      <div className="container-fluid">
        <ContainerLogs containerId={this.props.showingLog} showTimestamp={this.props.showingLogTimestamps}/>
      </div>
    );
  },

  renderContainerTable: function() {
    return (
      <div className="container">
        <h1 className="text-center">
          {this.props.title}
        </h1>
        <ContainerNav containers={this.props.containers} filter={this.props.inputFilter}/>
        <ContainerTable containers={this.props.containers} highlight={this.props.appliedFilter}/>
      </div>
    );
  }
});
