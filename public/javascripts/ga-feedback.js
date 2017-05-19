$(document).ready($(function () {

    var $feedbackLink = $("#beta-banner-feedback");

    $feedbackLink.on('click', function (e) {
        if (typeof ga === "function" && $feedbackLink != undefined) {
            e.preventDefault();
            var pageTitle = $('title').text();
            ga('send', 'event', 'itsa', 'feedback link clicked', pageTitle, {
                hitCallback: function () {
                    window.location = $feedbackLink.attr("href")
                }
            });
        }
    });

}));