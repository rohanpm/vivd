import React          from 'react';

import ContainerTable from './container-table';

export default React.createClass({
  render: function() {
    return (
      <div className="container">
        <h1 className="text-center">
          {this.props.title}
        </h1>
        <ContainerTable containers={this.props.containers}/>
      </div>
    );
  }
});
