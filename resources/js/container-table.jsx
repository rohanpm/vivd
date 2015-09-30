import React from 'react';

import ContainerRow from './container-row';

export default React.createClass({
  getDefaultProps: function() {
    return {
      containers: {data: []},
    };
  },

  containerRows: function() {
    return this.props.containers.data.map((c) => <ContainerRow currentUrl={this.props.currentUrl}
                                                               highlight={this.props.highlight}
                                                               key={c.id}
                                                               container={c}/>);
  },

  render: function() {
    return (
      <table className="table table-striped">
        <thead>
          <tr>
            <td>ID</td>
            <td>Git</td>
            <td>Last Used</td>
            <td>Status</td>
          </tr>
        </thead>
        <tbody>
          {this.containerRows()}
        </tbody>
      </table>
    );
  }
});
