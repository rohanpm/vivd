import React       from 'react';
import TimeAgo     from 'react-timeago';

import GlyphIcon       from './glyph-icon';
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

function parseGitLog(log) {
  try {
/*
commit ec4ec35961c8606521137d8abc49a6769966e22c
Author: Rohan McGovern <rohan@mcgovern.id.au>
Date:   Wed Nov 4 19:47:57 2015 +1000

    Expose git-log in API.
*/
    const lines = log.split("\n");
    const sha1 = lines[0].split(' ')[1];
    const authorLine = lines[1];
    const dateLine = lines[2];
    const subjectBrief = lines[4].trim();
    const subjectRest = lines.slice(5).join("\n");

    return {sha1, authorLine, dateLine, subjectBrief, subjectRest};
  } catch (e) {
    console.log(e);
    return null;
  }
}

export default React.createClass({
  getInitialState: function() {
    const c = this.props.container;
    const parsed = parseGitLog(c.attributes['git-log']);
    return {parsedGitLog: parsed,
            open:         parsed ? this.calculateOpen(parsed) : false};
  },

  calculateOpen: function({sha1, subjectBrief, authorLine, dateLine, subjectRest}) {
    const h = this.props.highlight;
    if (!h) {
      return false;
    }

    const hl = h.toLowerCase();

    if (sha1.includes(hl) || subjectBrief.toLowerCase().includes(hl)) {
      return false;
    }

    return (authorLine.toLowerCase().includes(hl) ||
            dateLine.toLowerCase().includes(hl) ||
            subjectRest.toLowerCase().includes(hl));
  },

  toggle: function() {
    this.setState({open: !this.state.open});
  },

  gitToggleElement: function() {
    return (
      <div className="git-toggler" onClick={this.toggle}>
        <GlyphIcon icon-type="chevron-up"/>
      </div>
    );
  },

  gitElementFromParsed: function({'git-ref': ref}, {sha1, authorLine, dateLine, subjectBrief, subjectRest}) {
    const abbrev = shortenedRef(ref);
    const topClass = this.state.open ? 'open' : 'closed';
    const subjectRestElem = subjectRest ? (
      <code>
        <MarkedText text={subjectRest} mark={this.props.highlight}/>
      </code>
    ) : null;

    return (
      <span className={topClass}>
        <abbr title={ref}>
          <MarkedText text={abbrev} mark={this.props.highlight}/>
        </abbr>
        <br/>
        <div className="git-commit-text">
          <code>
            commit
          </code>
        </div>
        <div className="git-revision">
          <code>
            <MarkedText text={sha1} mark={this.props.highlight}/>
          </code>
        </div>
        <div className="git-subject-brief">
          <code>
            <MarkedText text={subjectBrief} mark={this.props.highlight}/>
          </code>
        </div>
        <div className="git-author-line">
          <code>
            <MarkedText text={authorLine} mark={this.props.highlight}/>
          </code>
        </div>
        <div className="git-date-line">
          <code>
            <MarkedText text={dateLine} mark={this.props.highlight}/>
          </code>
        </div>
        <div className="git-subject-rest">
          {subjectRestElem}
        </div>
        {this.gitToggleElement()}
      </span>
    );
  },

  gitElement: function(params) {
    const {'git-ref': ref, 'git-revision': rev, 'git-oneline': oneline, 'git-log': log} = params;
    console.log(log);
    const parsed = this.state.parsedGitLog;
    if (parsed) {
      return this.gitElementFromParsed(params, parsed);
    }
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
          <ContainerButton container={c} currentUrl={this.props.currentUrl}/>
        </td>
      </tr>
    );
  },
});
