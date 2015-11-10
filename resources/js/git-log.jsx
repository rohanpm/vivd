import React from 'react';

import GlyphIcon  from './glyph-icon';
import MarkedText from './marked-text';

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
  if (!log) {
    return null;
  }
  try {
/*
commit ec4ec35961c8606521137d8abc49a6769966e22c
Author: Rohan McGovern <rohan@mcgovern.id.au>
Date:   Wed Nov 4 19:47:57 2015 +1000

    Expose git-log in API.
*/
    var lines          = log.split("\n");
    const sha1         = lines[0].split(' ')[1];
    lines              = lines.slice(1);

    const blankIdx     = lines.findIndex(s => (s == ""));
    // Cheat.  It's hard to make it look good with >2 lines of meta,
    // so keep a max of 2
    const meta         = lines.slice(0, blankIdx).slice(-2).join("\n");

    lines              = lines.slice(blankIdx + 1);
    
    const subjectBrief = lines[0].trimRight().slice(4);
    const subjectRest  = lines.slice(1).map(s => s.trimRight().slice(4)).join("\n");

    return {sha1, meta, subjectBrief, subjectRest};
  } catch (e) {
    console.log(e);
    return null;
  }
}

export default React.createClass({
  getInitialState: function() {
    const log    = this.props.gitLog;
    const parsed = parseGitLog(log);
    return {parsedGitLog: parsed,
            open:         parsed ? this.calculateOpen(parsed) : false};
  },

  calculateOpen: function({sha1, subjectBrief, meta, subjectRest}) {
    const h = this.props.highlight;
    if (!h) {
      return false;
    }

    const hl = h.toLowerCase();

    if (sha1.includes(hl) || subjectBrief.toLowerCase().includes(hl)) {
      return false;
    }

    return (meta.toLowerCase().includes(hl) ||
            subjectRest.toLowerCase().includes(hl));
  },

  toggle: function() {
    this.setState({open: !this.state.open});
  },

  toggleElement: function() {
    return (
      <div className="git-toggler" onClick={this.toggle}>
        <GlyphIcon icon-type="chevron-up"/>
      </div>
    );
  },

  subjectRestElement: function(text) {
    if (!text) {
      return null;
    }

    return (
      <code>
        <MarkedText text={text} mark={this.props.highlight}/>
      </code>
    );
  },

  gitElementParsed: function() {
    const abbrev   = shortenedRef(this.props.gitRef);
    const topClass = this.state.open ? 'open' : 'closed';
    const {sha1, meta, subjectBrief, subjectRest}
      = this.state.parsedGitLog;

    return (
      <span className={topClass}>
        <abbr title={this.props.gitRef}>
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
        <div className="git-meta">
          <code>
            <MarkedText text={meta} mark={this.props.highlight}/>
          </code>
        </div>
        <div className="git-subject-rest">
          {this.subjectRestElement(subjectRest)}
        </div>
        {this.toggleElement()}
      </span>
    );
  },

  // This element is used when the git log couldn't be parsed.
  // For example, the log may not yet be available.
  gitElementBasic: function() {
    const ref     = this.props.gitRef;
    const oneline = this.props.gitOneline;
    const rev     = this.props.gitRevision;

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
    if (this.state.parsedGitLog) {
      return this.gitElementParsed();
    }
    return this.gitElementBasic();
  },
});
