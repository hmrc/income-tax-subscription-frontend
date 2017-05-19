$(document).ready($(function () {

    var $feedbackLink = $("#beta-banner-feedback");

    $feedbackLink.on('click', function (e) {
        var pageTitle = $('title').text();
        ga('send', 'event', 'itsa', 'feedback link clicked', pageTitle);
    });

}));