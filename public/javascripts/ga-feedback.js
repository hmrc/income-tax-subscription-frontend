$(document).ready($(function () {

    var $feedbackLink = $("#beta-banner-feedback");

    $feedbackLink.on('click', function (e) {
        e.preventDefault();
        var pageTitle = $('title').text();
        if (typeof ga === "function" && $feedbackLink != undefined) {
            ga('send', 'event', 'itsa', 'feedback link clicked', pageTitle, {
                hitCallback: function () {
                    window.location = $feedbackLink.attr("href")
                }
            });
        }else {
            window.location = $feedbackLink.attr("href")
        }
    });

}));