import React   from 'react';
import TimeAgo from 'react-timeago';

import ContainerButton from './container-button';

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

function gitElement({'git-ref': ref, 'git-revision': rev, 'git-oneline': oneline}) {
  const abbrev = shortenedRef(ref);
  const detail = oneline || rev;

  return <span>
    <abbr title={ref}>{abbrev}</abbr>
    <br/>
    <code>{detail}</code>
  </span>;
}

export default React.createClass({
  rowForContainer: function(c) {
    return (
      <tr key={c.id}>
        <td><a href={c.links.app}>{c.id}</a></td>
        <td>{gitElement(c.attributes)}</td>
        <td><TimeAgo date={c.attributes.timestamp} title={c.attributes.timestamp}/></td>
        <td><ContainerButton container={c}/></td>
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
