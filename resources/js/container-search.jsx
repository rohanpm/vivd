import React from 'react';

import GlyphIcon from './glyph-icon';
import Dispatch  from './dispatch';

function debounce(f, delay) {
  var timeout;
  return function() {
    const args = arguments;
	const onTimeout = () => {
	  timeout = null;
	  f.apply(this, args);
	};
	clearTimeout(timeout);
	timeout = setTimeout(onTimeout, delay);
  };
}

export default React.createClass({
  // note: cannot debounce this function due to react reusing event objects.
  searchChanged: function(event) {
    this.applyFilter(event.target.value);
  },

  applyFilter: debounce(function(str) {
    Dispatch('filter-requested', str);
  }, 400),

  render: function() {
    return (
      <div className="input-group">
        <input type="text" onChange={this.searchChanged} className="form-control" placeholder="Search..." aria-describedby="container-search"/>
        <span className="input-group-addon" id="container-search">
          <GlyphIcon icon-type="search"/>
        </span>
      </div>
    );
  },
});
