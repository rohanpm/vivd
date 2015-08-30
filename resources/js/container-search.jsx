import React from 'react';

import GlyphIcon from './glyph-icon';
import Dispatch  from './dispatch';

export default React.createClass({
  searchChanged: function(event) {
    Dispatch('filter-requested', event.target.value);
  },

  render: function() {
    return (
      <div className="input-group">
        <input type="text" onChange={this.searchChanged} className="form-control"
          value={this.props.filter}
          placeholder="Search..." aria-describedby="container-search"/>
        <span className="input-group-addon" id="container-search">
          <GlyphIcon icon-type="search"/>
        </span>
      </div>
    );
  },
});
