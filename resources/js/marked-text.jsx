import React from 'react';

function regexpQuote(x) {
  return (x || "").toString().replace(/[-\\^$*+?.()|[\]{}]/g, "\\$&")
}

function applyMark(str, mark, out) {
  if (!mark || !str) {
    out.push(str);
    return;
  }

  const hr = new RegExp('^([^]*?)(' + regexpQuote(mark) + ')([^]*)$', 'i');
  const match = hr.exec(str);
  if (!match) {
    out.push(str);
    return;
  }

  const plain = match[1];
  const toMark = match[2];
  const rest = match[3];

  out.push(plain);
  out.push(
    <mark key={out.length}>{toMark}</mark>
  );
  return applyMark(rest, mark, out);
}

export default React.createClass({
  getDefaultProps: function() {
    return {text: "",
            mark: null};
  },

  getElements: function() {
    const out = [];
    applyMark(this.props.text, this.props.mark, out);
    return out;
  },

  render: function() {
    return (
      <span>
        {this.getElements()}
      </span>
    );
  },
});
