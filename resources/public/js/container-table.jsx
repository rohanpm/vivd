import React   from 'react';
import TimeAgo from 'react-timeago';

export default React.createClass({
  rowForContainer: function(c) {
    return (
      <tr key={c.id}>
        <td><a href={c.links.app}>{c.id}</a></td>
        <td>{c.attributes['git-ref']}</td>
        <td><TimeAgo date={c.attributes.timestamp} title={c.attributes.timestamp}/></td>
        <td>{c.attributes.status}</td>
      </tr>
    );
  },

  containerRows: function() {
    return this.props.containers.data.map(this.rowForContainer);
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
