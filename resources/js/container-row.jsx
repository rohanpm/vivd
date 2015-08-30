import React       from 'react';
import TimeAgo     from 'react-timeago';

import ContainerButton from './container-button';

function regexpQuote(x) {
  return (x || "").toString().replace(/[-\\^$*+?.()|[\]{}]/g, "\\$&")
}

function shortenedRef(ref) {
  const branchOrTag = new RegExp("^refs/(heads|tags)/(.+)$");
  var result;

  if ((result = branchOrTag.exec(ref))) {
    return result[2];
  }

  const gerrit = new RegExp("^refs/changes/\\d+/(\\d+)/(\\d+)$");
  if ((result = gerrit.exec(ref))) {
    let change = result[1];
    let ps = result[2];
    return `change ${change} patchset ${ps}`;
  }

  return ref;
}

export default React.createClass({
  applyHighlight: function(str, acc) {
    acc = acc || 1;

    const h = this.props.highlight;
    if (!h || !str) {
      return [str];
    }

    const hr = new RegExp('^([^]*?)(' + regexpQuote(h) + ')([^]*)$', 'i');
    const match = hr.exec(str);
    if (!match) {
      return [str];
    }

    const plain = match[1];
    const toHighlight = match[2];
    const rest = match[3];

    const out = [plain];
    out.push(
      <mark key={acc}>{toHighlight}</mark>
    );
    Array.prototype.push.apply(out, this.applyHighlight(rest, acc+1));

    return out;
  },

  gitElement: function({'git-ref': ref, 'git-revision': rev, 'git-oneline': oneline}) {
    const abbrev = shortenedRef(ref);
    const detail = oneline || rev;

    return (
      <span>
        <abbr title={ref}>{this.applyHighlight(abbrev)}</abbr>
        <br/>
        <code>{this.applyHighlight(detail)}</code>
      </span>
    );
  },

  render: function() {
    const c = this.props.container;
    return (
      <tr>
        <td>
          <a href={c.links.app}>{this.applyHighlight(c.id)}</a>
        </td>
        <td>
          {this.gitElement(c.attributes)}
        </td>
        <td>
          <TimeAgo date={c.attributes.timestamp} title={c.attributes.timestamp}/>
        </td>
        <td>
          <ContainerButton container={c}/>
        </td>
      </tr>
    );
  },
});
