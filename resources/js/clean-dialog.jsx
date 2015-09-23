import React from 'react';

import GlyphIcon from './glyph-icon';

export default React.createClass({
  getDefaultProps: function() {
    return {
      onCancel: function(){},
      onSubmit: function(){},
    };
  },

  render: function() {
    return (
      <span>
        <h1>Clean {this.props.container.id}?</h1>
        <p>
          Cleaning a container will delete all data associated with the container.
          This will reclaim space and reset the application to a known state.
        </p>
        <p>
          Container metadata will be retained. The container will be rebuilt on next use.
        </p>
        <span className="pull-right">
          <button className="btn btn-lg btn-warning" onClick={this.props.onSubmit}>
            <GlyphIcon icon-type="trash"/>
            &nbsp;
            Clean
          </button>
          &nbsp;
          <button className="btn" onClick={this.props.onCancel}>
            Cancel
          </button>
        </span>
      </span>
    );
  }
});
