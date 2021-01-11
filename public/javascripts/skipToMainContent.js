  // ---------------------------------------------------
  // Introduce direct skip link control, to work around voiceover failing of hash links
  // https://bugs.webkit.org/show_bug.cgi?id=179011
  // https://axesslab.com/skip-links/
  // ---------------------------------------------------
  $('.skiplink').click(function(e) {
    e.preventDefault();
    $(':header:first').attr('tabindex', '-1').focus();
  });
