import React          from 'react';

import ContainerTable from './container-table';
import ContainerNav   from './container-nav';

export default React.createClass({
  render: function() {
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
