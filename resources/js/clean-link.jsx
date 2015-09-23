import React from 'react';
import Modal from 'react-modal';

import Dispatch    from './dispatch';
import CleanDialog from './clean-dialog';

const ModalStyle = {
  content: {
    top                   : '30%',
    left                  : '50%',
    right                 : 'auto',
    bottom                : 'auto',
    marginRight           : '-50%',
    transform             : 'translate(-50%, -30%)',
    zIndex                : 1000,
  },
  overlay: {
    zIndex                : 500,
  },
};

export default React.createClass({
  getInitialState: function() {
    return {prompting: false};
  },

  requestClean: function(evt) {
    evt.preventDefault();
    Dispatch('post-link', this.props.container.links.clean);
    this.closeModal();
  },

  openModal: function() {
    this.setState({prompting: true});
  },

  closeModal: function() {
    this.setState({prompting: false});
  },

  clicked: function(evt) {
    if (this.props.onClick) {
      this.props.onClick(evt);
    }
    this.openModal();
  },

  render: function() {
    return (
      <a onClick={this.clicked} href="#">
        Clean...
        <Modal
          isOpen={this.state.prompting}
          onRequestClose={this.closeModal}
          style={ModalStyle}>
          <CleanDialog container={this.props.container}
            onCancel={this.closeModal}
            onSubmit={this.requestClean}/>
        </Modal>
      </a>
    );
  }
});
