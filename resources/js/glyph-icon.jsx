import React from 'react';

export default React.createClass({
  render: function() {
    const iconType = this.props['icon-type'];
    const other = Object.assign({}, this.props);
    delete other['icon-type'];
    return (
      <span className={"glyphicon glyphicon-" + this.props['icon-type']} {...other}/>
    );
  },
});
