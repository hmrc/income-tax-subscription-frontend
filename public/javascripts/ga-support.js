$(document).ready($(function () {

    var $helpButton = $("#get-help-action");

    $helpButton.on('click', function (e) {
        var checkExist = setInterval(function() {
            var reportSubmit = $('#report-submit');
            if (reportSubmit.length) {
                reportSubmit.click(function(){
                    var supportLinkClickedTitle = $('title').text();
                    ga('send', 'event', 'itsa', 'support ticket submitted', supportLinkClickedTitle);
                });
                clearInterval(checkExist);
            }
        }, 100);

    });

}));