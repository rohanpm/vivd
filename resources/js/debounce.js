export default function(f, delay) {
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
