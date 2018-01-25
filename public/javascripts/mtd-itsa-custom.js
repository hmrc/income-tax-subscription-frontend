$(document).ready(function () {

    // add keystroke behaviour for anchors with role="button"
    // this is mainly for JAWS screen reader
    $('body').on('keypress', '[role="button"]', function (e) {
        if ((e.which === 13) || (e.which === 32)) {
            e.preventDefault();
            this.click();
        }
    });

    if (typeof GOVUK.ShowHideContent !== 'undefined') {
      var showHideContent = new GOVUK.ShowHideContent();
      showHideContent.init();
    }

});
