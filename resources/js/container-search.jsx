import React from 'react';

import GlyphIcon from './glyph-icon';
import Dispatch  from './dispatch';

export default React.createClass({
  searchChanged: function(event) {
    Dispatch('filter-requested', event.target.value);
  },

  clearSearch: function(event) {
    Dispatch('filter-requested', '');
  },

  render: function() {
    return (
      <div className="input-group">
        <span className="input-group-addon">
          <GlyphIcon icon-type="search"/>
        </span>
        <input type="text" onChange={this.searchChanged} className="form-control"
          value={this.props.filter}
          placeholder="Search..."/>
        {
          (() => {
            if (this.props.filter) {
              return (
                <span className="input-group-addon" style={{cursor: 'pointer'}} onClick={this.clearSearch}>
                  <GlyphIcon icon-type="remove"/>
                </span>
              );
            }
          })()
         }
      </div>
    );
  },
});
