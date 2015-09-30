import React from 'react';

import GlyphIcon  from './glyph-icon';
import Dispatch   from './dispatch';
import * as Links from './links';

export default React.createClass({
  searchChanged: function(event) {
    Dispatch('filter-requested', event.target.value);
  },

  clearSearch: function(event) {
    event.preventDefault();
    Dispatch('filter-requested', '');
  },

  render: function() {
    return (
      <div className="input-group">
        <span className="input-group-addon">
          <GlyphIcon icon-type="search"/>
        </span>
        <form method="get" action="">
          <input type="text" name="filter[*]" onChange={this.searchChanged} className="form-control"
            value={this.props.filter}
            autoComplete="off"
            placeholder="Search..."/>
        </form>
        {
          (() => {
            if (this.props.filter) {
              return (
                <span className="input-group-addon" style={{cursor: 'pointer'}}>
                  <a href={Links.urlWithParams(this.props.currentUrl, {'filter[*]': null})}
                     style={{color: '#555'}}
                     onClick={this.clearSearch}>
                    <GlyphIcon icon-type="remove"/>
                  </a>
                </span>
              );
            }
          })()
         }
      </div>
    );
  },
});
