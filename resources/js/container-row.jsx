import React       from 'react';
import TimeAgo     from 'react-timeago';

import GitLog          from './git-log';
import ContainerButton from './container-button';
import MarkedText      from './marked-text';

export default React.createClass({
  render: function() {
    const c       = this.props.container;
    const attr    = c.attributes;
    const gitAttr = {gitRef:      attr['git-ref'],
                     gitRevision: attr['git-revision'],
                     gitOneline:  attr['git-oneline'],
                     gitLog:      attr['git-log']};
    return (
      <tr>
        <td className="id">
          <a href={c.links.app}>
            <MarkedText text={c.id} mark={this.props.highlight}/>
          </a>
        </td>
        <td className="git">
          <GitLog highlight={this.props.highlight} {...gitAttr}/>
        </td>
        <td className="timestamp">
          <TimeAgo date={c.attributes.timestamp} title={c.attributes.timestamp}/>
        </td>
        <td className="status">
          <ContainerButton container={c} currentUrl={this.props.currentUrl}/>
        </td>
      </tr>
    );
  },
});
