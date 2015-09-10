// ==UserScript==
// @name        gerrit-vivd
// @namespace   vivd
// @description Adds links to vivd container in gerrit UI
// @include     https://GERRIT.EXAMPLE.COM/*
// @version     1
// @grant       GM_xmlhttpRequest
// @require     http://code.jquery.com/jquery-2.1.4.min.js
// ==/UserScript==
//
// This greasemonkey script may be installed to automatically link from
// a Gerrit change screen to vivd.
//
// To use it, you'll first need to adjust these three variables to match your
// Gerrit and vivd setup:
//
// - @include (above)
// - project (below)
// - vivdBaseUrl (below)

// Gerrit normally doesn't have jQuery loaded, and also uses '$' for something else.
jQuery.noConflict();

(function($){
  var sha1Pattern = /[0-9a-fA-F]{40}/,
      project     = 'SOME-PROJECT',
      vivdBaseUrl = "http://VIVD.EXAMPLE.COM/";

  // Return true if we may be looking at an <project> patch
  function projectMatch() {
    return $("tr:contains('Project'):contains('" + project + "')").length > 0;
  }

  // Return SHA1 of current patch set (if we are on change screen).
  function findSha1() {
    var commitRow = $('td div:contains("Commit")').closest('tr:not(:contains("Reviewers"))');
    if (commitRow.length == 0) {
      console.log("Cannot find displayed Commit.");
      return;
    }

    var text = commitRow.text(),
        match = text.match(sha1Pattern);

    if (match) {
      return match[0];
    }

    console.log("Cannot find SHA1 within Commit row.");
  }

  // Find the element linking to gitweb, for a particular SHA1.
  // (vivd link will be placed near this)
  function findGitwebAnchor(sha1) {
    return $('a:contains("(gitweb)")[href*="' + sha1 + '"]');
  }

  // URL to search for containers matching a certain term, in UI.
  function vivdSearchUrl(term) {
    return vivdBaseUrl + "?filter[*]=" + encodeURIComponent(term);
  }

  // URL to search for containers matching a certain term, in API.
  function vivdApiSearchUrl(term) {
    return vivdBaseUrl + "a/containers?page[limit]=1&filter[*]=" + encodeURIComponent(term);
  }

  // Extract href from JSON API link object
  // (which can be an object or a string).
  function jsonApiLinkUrl(object) {
    if (object.hasOwnProperty('href')) {
      return object.href;
    }
    return object;
  }

  // Given a container object, reset any inserted links for that revision to directly
  // point to the application within the container.
  function resolveContainerLink(c) {
    var revision = c.attributes['git-revision'],
        url      = jsonApiLinkUrl(c.links.app),
        elems    = $('a.gm-vivd');

    elems.each(function(idx, elem) {
      var elem = $(elem);
      if (elem.data('revision') == revision) {
        elem.attr('href', url);
      }
    });
  }

  // Given a response to a vivd search, resolve all pending container links.
  function resolveContainerLinks(response) {
    var doc = JSON.parse(response.response),
        containers = doc.data;
    containers.forEach(resolveContainerLink);
  }

  // Top-level handler invoked whenever inserting vivd links might be needed.
  function addVivdLinks() {
    // Bail out if any vivd link already exists
    // (very important - this is what prevents infinite recursion!)
    if ($('a.gm-vivd').length > 0) {
      return;
    }

    // Bail out unless looking at the right project
    if (!projectMatch()) {
      return;
    }

    var sha1 = findSha1();
    if (!sha1) {
      return;
    }

    var container = findGitwebAnchor(sha1).parent(),
        url       = vivdSearchUrl(sha1),
        link      = $("<a>").data('revision', sha1).attr('class', 'gm-vivd').attr('href', url).text('(vivd)');

    // Append the "(vivd)" link next to "(gitweb)".
    // Currently, it points to the UI pre-filled to search for this commit.
    container.append(' ').append(link);

    // Try to directly resolve the URL of the container, so you can click through directly to it,
    // rather than having to click through the search UI.  (Only works if container exists
    // when you've loaded this page).
    GM_xmlhttpRequest({
      method:  'GET',
      headers: {'Accept': 'application/vnd.api+json'},
      url:     vivdApiSearchUrl(sha1),
      onload:  resolveContainerLinks,
    });
  }

  // Installs event listeners.
  function listenForEvents() {
    var body = $('body')[0];
    if (!body) {
      console.warn("no body found.");
      return;
    }

    // I could not find any kind of hook to add a callback when gerrit loads a new change.
    // Here, we assume that any DOM elements added/removed anywhere under body means
    // we should scan to see if we're on a change screen.
    // (The performance seems OK.)
    var mut = new MutationObserver(addVivdLinks);
    mut.observe(body, {childList: true, subtree: true});
  }

  listenForEvents();
})(jQuery);
