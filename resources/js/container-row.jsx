import React       from 'react';
import TimeAgo     from 'react-timeago';

import ContainerButton from './container-button';
import MarkedText      from './marked-text';

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
  gitElement: function({'git-ref': ref, 'git-revision': rev, 'git-oneline': oneline}) {
    const abbrev = shortenedRef(ref);
    const detail = oneline || rev;

    return (
      <span>
        <abbr title={ref}>
          <MarkedText text={abbrev} mark={this.props.highlight}/>
        </abbr>
        <br/>
        <code>
          <MarkedText text={detail} mark={this.props.highlight}/>
        </code>
      </span>
    );
  },

  render: function() {
    const c = this.props.container;
    return (
      <tr>
        <td className="id">
          <a href={c.links.app}>
            <MarkedText text={c.id} mark={this.props.highlight}/>
          </a>
        </td>
        <td className="git">
          {this.gitElement(c.attributes)}
        </td>
        <td className="timestamp">
          <TimeAgo date={c.attributes.timestamp} title={c.attributes.timestamp}/>
        </td>
        <td className="status">
          <ContainerButton container={c}/>
        </td>
      </tr>
    );
  },
});
